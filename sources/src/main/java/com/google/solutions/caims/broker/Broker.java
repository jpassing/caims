package com.google.solutions.caims.broker;

import com.google.solutions.caims.workload.AttestationToken;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Broker {
  private final @NotNull HttpServer server;

  /** Current set of registrations, continuously updated by the daemon */
  private volatile Set<Registration> registrations = Set.of();

  public Broker(
    int listenPort,
    int threadPoolSize
  ) throws GeneralSecurityException, IOException {
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
      System.out.println("[INFO] Received request");

      try (var responseStream = new DataOutputStream(exchange.getResponseBody())) {
        exchange.sendResponseHeaders(200, 0);
        exchange
          .getResponseHeaders()
          .set("Content-Type", "text/plain");
        responseStream.writeUTF(this.registrations
          .stream()
          .map(r -> r.instanceName())
          .collect(Collectors.joining("\n")));
      }
    });
  }

  void refreshRegistrations(
    @NotNull Set<Registration> registrations
  ) {
    this.registrations = registrations;
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
