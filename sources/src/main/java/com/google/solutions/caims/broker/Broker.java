package com.google.solutions.caims.broker;

import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.solutions.caims.AbstractServer;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    this.mapGetJson("/", () -> handleTokensRequest());
    this.<Map<RequestToken, EncryptedMessage>, EncryptedMessage>mapPostJson(
      "/forward",
      new TypeToken<Map<RequestToken, EncryptedMessage>>() {}.getType(),
      (Map<RequestToken, EncryptedMessage> request) -> handleInferenceRequest(request));

  }

  private List<RequestToken> handleTokensRequest() {
    //
    // Take a random sample of registrations and return a token for each.
    //
    return this.registrations.stream()
      .sorted(Comparator.comparingDouble(x -> RANDOM.nextInt(32)))
      .limit(this.maxRequestTokens)
      .map(r -> new RequestToken(r.attestationToken))
      .toList();
  }

  private @Nullable EncryptedMessage handleInferenceRequest(
    @NotNull Map<RequestToken, EncryptedMessage> requests
  )  {
    Preconditions.checkNotNull(requests, "requests");

    for (var item : requests.entrySet()) {
      try {
        var tokenPayload = item.getKey()
          .attestationToken()
          .verify(
            this.brokerId.toString(),
            this.requireProductionAttestations);

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
      }
      catch (TokenVerifier.VerificationException e) {
        //
        // Token invalid or expired, try next.
        //
        continue;

      }
      // TODO: Forward request to instance
    }

    return null;
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

//
//  public record TokensResponse(
//    @NotNull List<RequestToken> tokens
//  ) {}
//
//  /**
//   * Request to the broker.
//   *
//   * @param workloadRequests Zero or more workload requests, each targeting a specific workload instance
//   */
//  public record Request(
//    @NotNull List<WorkloadRequest> workloadRequests
//  ) {}
//
//  /**
//   * Request to a specific workload instance.
//   *
//   * @param requestToken Token for the instance.
//   * @param encryptedMessage E2EE-encrypted message, encoded as base64.
//   */
//  public record WorkloadRequest(
//    @NotNull String requestToken,
//    @NotNull String encryptedMessage
//  ) {}
//
//  /**
//   * Response from the broker
//   *
//   * @param encryptedResponseMessage E2EE-encrypted response message from the workload instance
//   * @param requestTokens A new set of requests that the client can use for future requests
//   */
//  public record Response(
//    @Nullable String encryptedResponseMessage,
//    @NotNull List<String> requestTokens
//    ) {}
}
