package com.google.solutions.caims.broker;

import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.TokenVerifier;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.workload.AttestationToken;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Broker {

  /** Charset used in messages and HTTP responses */
  private static final @NotNull Charset CHARSET = StandardCharsets.UTF_8;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final @NotNull GsonFactory GSON = new GsonFactory();
  private final @NotNull Broker.Identifier brokerId;
  private final @NotNull HttpServer server;

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
    this.brokerId = brokerId;
    this.maxRequestTokens = maxRequestTokens;
    this.requireProductionAttestations = requireProductionAttestations;

    //
    // Start an HTTP server and listen for inference requests.
    //
    this.server = HttpServer.create(new InetSocketAddress(listenPort), 0);
    this.server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

    System.out.printf(
      "[INFO] Listening on port %d, using a maximum of %d threads\n",
      listenPort,
      threadPoolSize);

    server.createContext("/", exchange -> {
      try (var writer = new OutputStreamWriter(exchange.getResponseBody(), CHARSET)) {
        switch (exchange.getRequestMethod()) {
          case "GET": {
            exchange.sendResponseHeaders(200, 0);
            exchange
              .getResponseHeaders()
              .set("Content-Type", "application/json; charset=" + CHARSET.name());

            writer.write(GSON.toString(handleTokensRequest()));
          }

          default: {
            exchange.sendResponseHeaders(405, 0);
          }
        }
      }
      catch (Exception e) {
        exchange.sendResponseHeaders(500, 0);
        System.err.printf("[ERROR] %s\n", e.getMessage());
      }
    });
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

  private EncryptedMessage handleInferenceRequest(
    @NotNull Map<RequestToken, EncryptedMessage> requests
  ) throws TokenVerifier.VerificationException {
    for (var item : requests.entrySet()) {
      var tokenPayload = item.getKey().attestationToken().verify(
        this.brokerId.toString(),
        this.requireProductionAttestations);

    }
    throw new RuntimeException("NIY");
  }

  /**
   * Start HTTP server on a background thread.
   */
  public final void start() {
    this.server.start();
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
