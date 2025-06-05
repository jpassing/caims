package com.google.solutions;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Server {
  private final @NotNull HttpServer server;

  private static final @NotNull Charset CHARSET = StandardCharsets.UTF_8;

  public Server(int listenPort, int threadPoolSize) throws IOException {
    this.server = HttpServer.create(new InetSocketAddress(listenPort), 0);
    this.server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

    System.out.printf("Listening on port %d, using a maximum of %d threads\n", listenPort, threadPoolSize);

    server.createContext("/", exchange -> {
      try (exchange; var writer = new OutputStreamWriter(exchange.getResponseBody(), CHARSET)) {
        //
        // Check HTTP method. We expect the request to be sent by POST.
        //
        if (exchange.getRequestMethod().equals("POST")) {
          exchange.sendResponseHeaders(200, 0);exchange
            .getResponseHeaders()
            .set("Content-Type", "text/plain; charset=" + CHARSET.name());

          writer.write("Hello.");
        }
        else {
          exchange.sendResponseHeaders(405, 0);
          exchange
            .getResponseHeaders()
            .set("Content-Type", "text/plain; charset=" + CHARSET.name());

          writer.write("Invalid HTTP method, use HTTP POST instead.");
        }
      }

      // decrypt request using REK
      // run inference
      // derive key for response
      // encrypt response



    });
  }


  /**
   * Start HTTP server on a background thread.
   */
  public final void start() {
    this.server.start();
  }
}
