package com.google.solutions.caims.broker;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.crypto.tink.subtle.Base64;
import com.google.solutions.caims.AbstractServer;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Broker extends AbstractServer {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final @NotNull Broker.Endpoint brokerId;

  /** Current set of registrations, continuously updated by the daemon */
  private volatile Set<Registration> registrations = Set.of();

  /** Max number of request tokens returned to a client */
  private final int maxRequestTokens;

  public Broker(
    @NotNull Broker.Endpoint brokerId,
    int listenPort,
    int threadPoolSize,
    int maxRequestTokens
  ) throws IOException {
    super(listenPort, threadPoolSize);

    this.brokerId = brokerId;
    this.maxRequestTokens = maxRequestTokens;

    //
    // Register HTTP endpoints.
    //
    this.mapGetJson("/", () -> getTokens());
    this.mapPostJson(
      "/forward",
      new TypeToken<List<WorkloadRequest>>() {}.getType(),
      (List<WorkloadRequest> request) -> forwardInferenceRequest(request));
  }

  /**
   * Take a random sample of registrations and return a token for each. Clients
   * can then use one or more of these tokens to make requests to the /forward
   * endpoint.
   */
  private List<RequestToken> getTokens() {
    return this.registrations.stream()
      .sorted(Comparator.comparingDouble(x -> RANDOM.nextInt(32)))
      .limit(this.maxRequestTokens)
      .map(r -> new RequestToken(r.attestationToken))
      .toList();
  }

  /**
   * Dispatch an encrypted inference requests by forwarding it to an available
   * workload instance.
   */
  private @NotNull WorkloadResponse forwardInferenceRequest(
    @NotNull List<WorkloadRequest> requests
  )  {
    Preconditions.checkNotNull(requests, "requests");

    for (var request : requests) {
      //
      // Verify token to ensure the request is legitimate and to find out
      // which instance it belongs to.
      //
      // NB. Here, we don't care whether attestation are production or
      //     debug - this is up to the client to decide.
      //
      AttestationToken.Payload tokenPayload;
      try {
        tokenPayload = request.requestToken()
          .attestationToken()
          .verify(
            this.brokerId.toString(),
            false);
      }
      catch (TokenVerifier.VerificationException e) {
        //
        // Token invalid or expired, try next.
        //
        continue;
      }

      //
      // Verify that the corresponding instance is (still) registered. Instances may come and
      // go at any time, so it's possible that it's no longer available.
      //
      var registration = this.registrations.stream()
        .filter(r ->
          r.instanceName.equals(tokenPayload.instanceName()) &&
            r.zone.equals(tokenPayload.instanceZone()) &&
            r.projectId.equals(tokenPayload.projectId()))
        .findFirst();
      if (registration.isEmpty()) {
        //
        // This instance is no longer registered, try next.
        //
        continue;
      }

      //
      // Forward request.
      //
      var url = new GenericUrl(String.format(
        "http://%s.%s.c.%s.internal:8080/",
        registration.get().instanceName,
        registration.get().zone,
        registration.get().projectId));

      System.out.printf(
        "[INFO] Forwarding inference request to %s (%d bytes)\n",
        url,
        request.encryptedMessage().cipherText().length);

      try {
        var response = new NetHttpTransport()
          .createRequestFactory()
            .buildPostRequest(url, new ByteArrayContent(
              "binary/octet-stream",
              request.encryptedMessage().cipherText()))
            .setThrowExceptionOnExecuteError(true)
            .execute();

        try (var stream = response.getContent()) {
          return new WorkloadResponse(new EncryptedMessage(stream.readAllBytes()));
        }
      }
      catch (IOException e) {
        System.err.printf(
          "[ERROR] Forwarding inference request failed: %s\n",
          e.getMessage());
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    throw new IllegalArgumentException(
      "None of the requested workload instance are available any more");
  }

  void refreshRegistrations(
    @NotNull Set<Registration> registrations
  ) {
    this.registrations = registrations;
  }

  /**
   * Endpoint of the broker, used as audience in tokens.
   */
  public record Endpoint(
    @NotNull String url
  ) {
    public Endpoint(
      @NotNull String projectNumber,
      @NotNull String region
    ) {
      this(String.format("https://broker-%s.%s.run.app/", projectNumber, region));
    }
    @Override
    public String toString() {
      return this.url;
    }
  }

  /**
   * A registered node that is ready to handle requests.
   */
  public record Registration(
    @NotNull String projectId,
    @NotNull String zone,
    @NotNull String instanceName,
    @NotNull AttestationToken attestationToken
    ) {}

  /**
   * Request to a particular workload.
   *
   * @param token Request token
   * @param message Encrypted message, base64-encoded
   */
  public record WorkloadRequest(
    @NotNull String token,
    @NotNull String message
  ) {
    public WorkloadRequest(
      @NotNull RequestToken token,
      @NotNull EncryptedMessage message) {
      this(token.attestationToken().token(), Base64.encode(message.cipherText()));
    }

    @NotNull RequestToken requestToken() {
      return  new RequestToken(this.token);
    }

    @NotNull EncryptedMessage encryptedMessage() {
      return new EncryptedMessage(Base64.decode(this.message));
    }
  }

  /**
   * Response from a workload.
   *
   * @param message Encrypted message, base64-encoded
   */
  public record WorkloadResponse(
    @NotNull String message
  ) {
    public WorkloadResponse(EncryptedMessage message) {
      this(Base64.encode(message.cipherText()));
    }

    public @NotNull EncryptedMessage toEncryptedMessage() {
      return new EncryptedMessage(Base64.decode(this.message));
    }
  }
}
