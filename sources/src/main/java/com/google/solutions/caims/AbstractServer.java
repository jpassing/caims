package com.google.solutions.caims;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for HTTP servers.
 */
public abstract class AbstractServer {

  /** Charset used in messages and HTTP responses */
  protected static final @NotNull Charset CHARSET = StandardCharsets.UTF_8;
  private final @NotNull HttpServer server;
  private static final @NotNull Gson GSON = new Gson();

  protected AbstractServer(
    int listenPort,
    int threadPoolSize
  ) throws IOException {

    //
    // Start an HTTP server and listen for requests.
    //
    this.server = HttpServer.create(new InetSocketAddress(listenPort), 0);
    this.server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

    System.out.printf(
      "[INFO] Listening on port %d, using a maximum of %d threads\n",
      listenPort,
      threadPoolSize);
  }

  /** Register a GET endpoint that returns JSON output */
  protected <TResponse> void mapGetJson(
    @NotNull String path,
    @NotNull Supplier<Object> handler
    ) {
    this.server.createContext(path, exchange -> {
      try (var writer = new OutputStreamWriter(exchange.getResponseBody(), CHARSET)) {
        exchange
          .getResponseHeaders()
          .set("Content-Type", "text/plain; charset=" + CHARSET.name());

        //
        // Check HTTP method.
        //
        if (!"GET".equals(exchange.getRequestMethod())) {
          exchange.sendResponseHeaders(405, 0);
          writer.write("Method not supported");
          return;
        }

        //
        // Send response.
        //
        try {
          var responseBody = handler.get();
          exchange.sendResponseHeaders(200, 0);
          writer.write(GSON.toJson(responseBody));
        }
        catch (Exception e) {
          exchange.sendResponseHeaders(500, 0);
          writer.write("Internal server error");
          System.err.printf("[ERROR] %s\n", e.getMessage());
        }
      }
    });
  }

  /** Register a POST endpoint that receives and returns JSON output */
  protected <TRequest, TResponse> void mapPostJson(
    @NotNull String path,
    @NotNull Type requestType,
    @NotNull Function<TRequest, TResponse> handler
  ) {
    this.server.createContext(path, exchange -> {
      try (
        var reader = new InputStreamReader(exchange.getRequestBody());
        var writer = new OutputStreamWriter(exchange.getResponseBody(), CHARSET)
      ) {
        exchange
          .getResponseHeaders()
          .set("Content-Type", "application/json; charset=" + CHARSET.name());

        //
        // Check HTTP method.
        //
        if (!"POST".equals(exchange.getRequestMethod())) {
          exchange.sendResponseHeaders(405, 0);
          writer.write("Method not supported");
          return;
        }

        try {
          //
          // Parse request.
          //
          var requestBody = (TRequest)GSON.fromJson(reader, requestType);
          Preconditions.checkArgument(requestBody != null);

          //
          // Send response.
          //
          var responseBody = handler.apply(requestBody);
          exchange.sendResponseHeaders(200, 0);
          writer.write(GSON.toJson(responseBody));
        }
        catch (IllegalArgumentException e) {
          exchange.sendResponseHeaders(400, 0);
          writer.write("Invalid arguments");
          System.err.printf("[ERROR] %s\n", e.getMessage());
        }
        catch (Exception e) {
          exchange.sendResponseHeaders(500, 0);
          writer.write("Internal server error");
          System.err.printf("[ERROR] %s\n", e.getMessage());
        }
      }
    });
  }

  protected void mapPostEncrypted(
    @NotNull String path,
    int maxMessageSize,
    @NotNull Function<EncryptedMessage, EncryptedMessage> handler
  ) {
    this.server.createContext(path, exchange -> {
      try (
        var reader = new InputStreamReader(exchange.getRequestBody());
        var writer = new OutputStreamWriter(exchange.getResponseBody(), CHARSET)
      ) {
        exchange
          .getResponseHeaders()
          .set("Content-Type", "binary/octet-stream");

        //
        // Check HTTP method.
        //
        if (!"POST".equals(exchange.getRequestMethod())) {
          exchange.sendResponseHeaders(405, 0);
          return;
        }

        try (var requestStream = new DataInputStream(exchange.getRequestBody());
             var responseStream = new DataOutputStream(exchange.getResponseBody())) {
          //
          // Parse request.
          //
          var requestMessage = EncryptedMessage.read(requestStream, maxMessageSize);
          Preconditions.checkArgument(requestMessage != null);

          //
          // Send response.
          //
          var responseMessage = handler.apply(requestMessage);
          exchange.sendResponseHeaders(200, 0);
          responseMessage.write(responseStream);
        }
        catch (IllegalArgumentException e) {
          exchange.sendResponseHeaders(400, 0);
          writer.write("Invalid arguments");
          System.err.printf("[ERROR] %s\n", e.getMessage());
        }
        catch (Exception e) {
          exchange.sendResponseHeaders(500, 0);
          writer.write("Internal server error");
          System.err.printf("[ERROR] %s\n", e.getMessage());
        }
      }
    });
  }

  /**
   * Start HTTP server on a background thread.
   */
  public final void start() {
    this.server.start();
  }
}
