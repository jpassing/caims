package com.google.solutions.caims.broker;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.solutions.caims.AbstractServer;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Broker extends AbstractServer {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final @NotNull Broker.Identifier brokerId;

  /** Current set of registrations, continuously updated by the daemon */
  private volatile Set<Registration> registrations = Set.of();

  /** Max number of request tokens returned to a client */
  private final int maxRequestTokens;

  private final boolean requireProductionAttestations;

  public Broker(
    @NotNull Identifier brokerId,
    int listenPort,
    int threadPoolSize,
    int maxRequestTokens,
    boolean requireProductionAttestations
  ) throws IOException {
    super(listenPort, threadPoolSize);

    this.brokerId = brokerId;
    this.maxRequestTokens = maxRequestTokens;
    this.requireProductionAttestations = requireProductionAttestations;

    //
    // Register HTTP endpoints.
    //
    this.mapGetJson("/", () -> getTokens());
    this.mapPostJson(
      "/forward",
      new TypeToken<Map<RequestToken, EncryptedMessage>>() {}.getType(),
      (Map<RequestToken, EncryptedMessage> request) -> forwardInferenceRequest(request));
  }

  /**
   * Take a random sample of registrations and return a token for each. Clients
   * can then use one or more of these tokens to make requests to the forward-
   * endpoint.
   */
  private List<RequestToken> getTokens() {
    //
    //
    //
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
  private @Nullable EncryptedMessage forwardInferenceRequest(
    @NotNull Map<RequestToken, EncryptedMessage> requests
  )  {
    Preconditions.checkNotNull(requests, "requests");

    for (var item : requests.entrySet()) {
      //
      // Verify token to ensure the request is legitimate and to find out
      // which instance it belongs to.
      //
      AttestationToken.Payload tokenPayload;
      try {
        tokenPayload = item.getKey()
          .attestationToken()
          .verify(
            this.brokerId.toString(),
            this.requireProductionAttestations);
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
      if (!registration.isPresent()) {
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

      System.out.printf("[INFO] Forwarding inference request to %s\n", url);

      try {
        var response = new NetHttpTransport()
          .createRequestFactory()
            .buildPostRequest(url, new ByteArrayContent(
              "binary/octet-stream",
              item.getValue().cipherText()))
            .setThrowExceptionOnExecuteError(true)
            .execute();

        return EncryptedMessage.read(new DataInputStream(response.getContent()));
      }
      catch (IOException e) {
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
   * Unique identifier of the broker, used as audience in tokens.
   * @param projectNumber
   */
  public record Identifier(
    @NotNull String projectNumber
  ) {
    @Override
    public String toString() {
      return String.format("urn:com:google:solutions:caims:%s", this.projectNumber);
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
}
