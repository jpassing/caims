package com.google.solutions.caims;

import com.google.common.base.Preconditions;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.protocol.Message;
import com.google.solutions.caims.protocol.RequestEncryptionKeyPair;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

public class Server {
  private final @NotNull HttpServer server;
  private final @NotNull RequestEncryptionKeyPair keyPair;
  private static final @NotNull Charset CHARSET = StandardCharsets.UTF_8;

  private static final int MAX_MESSAGE_SIZE = 1024;

  public Server(
    int listenPort,
    int threadPoolSize
  ) throws GeneralSecurityException, IOException {
    //
    // Generate a new key pair. The key pair remains valid for the lifetime
    // of the process. Clients use the pair's public key to encrypt their
    // requests.
    //
    this.keyPair = RequestEncryptionKeyPair.generate();

    //
    // Start a HTTP server and listen for inference requests.
    //
    this.server = HttpServer.create(new InetSocketAddress(listenPort), 0);
    this.server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

    System.out.printf("Listening on port %d, using a maximum of %d threads\n", listenPort, threadPoolSize);

    server.createContext("/", exchange -> {
      //
      // Check HTTP method. We expect the request to be sent by POST.
      //
      if (!exchange.getRequestMethod().equals("POST")) {
        exchange.sendResponseHeaders(405, 0);
        exchange
          .getResponseHeaders()
          .set("Content-Type", "text/plain; charset=" + CHARSET.name());

        try (var writer = new OutputStreamWriter(exchange.getResponseBody(), CHARSET)) {
          writer.write("Invalid HTTP method, use HTTP POST instead.");
        }
        return;
      }

      //
      // Handle an inference request.
      //
      try (var requestStream = new DataInputStream(exchange.getRequestBody());
           var responseStream = new DataOutputStream(exchange.getResponseBody())) {

        var requestMessage = EncryptedMessage.read(requestStream, MAX_MESSAGE_SIZE);
        var responseMessage = handleInferenceRequest(requestMessage);

        exchange.sendResponseHeaders(200, 0);
        exchange
          .getResponseHeaders()
          .set("Content-Type", "binary/octet-stream");

        responseMessage.write(responseStream);
      }
      catch (Exception e) {
        exchange.sendResponseHeaders(500, 0);
        System.err.printf("[ERROR] %s\n", e.getMessage());
      }
    });
  }

  private EncryptedMessage handleInferenceRequest(
    @NotNull EncryptedMessage encryptedRequest
  ) throws GeneralSecurityException, IOException {
    System.out.println("[INFO] Handling inference request...");

    var request = encryptedRequest.decrypt(this.keyPair.privateKey());
    Preconditions.checkNotNull(
      request.senderPublicKey(),
      "The client did not provide a public key");

    var response = new Message(
      String.format(
        "> %s\nCan't help you there, but I won't tell anybody you asked.",
        request.toString()),
      null);
    return response.encrypt(request.senderPublicKey());
  }

  /**
   * Start HTTP server on a background thread.
   */
  public final void start() {
    this.server.start();
  }
}
