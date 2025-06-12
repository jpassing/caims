package com.google.solutions.caims.client;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.solutions.caims.broker.Broker;
import com.google.solutions.caims.broker.RequestToken;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.protocol.Message;
import com.google.solutions.caims.protocol.RequestEncryptionKeyPair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Client {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final GsonFactory GSON_FACTORY = new GsonFactory();
  private static final Gson GSON = new Gson();
  private static final HttpRequestFactory HTTP_FACTORY = new NetHttpTransport().createRequestFactory();
  private final @NotNull Broker.Endpoint endpoint;
  private final boolean debug;

  public Client(@NotNull Broker.Endpoint endpoint, boolean debug) {
    this.endpoint = endpoint;
    this.debug = debug;
  }

  private List<RequestToken> getTokens() throws IOException {
    var response = HTTP_FACTORY
      .buildGetRequest(new GenericUrl(this.endpoint.url()))
      .setParser(new JsonObjectParser(GSON_FACTORY))
      .execute();

    try (var reader = new InputStreamReader(response.getContent(), response.getContentCharset())) {
      return GSON.fromJson(reader, new TypeToken<List<RequestToken>>() {}.getType());
    }
  }

  private EncryptedMessage forward(List<Broker.WorkloadRequest> requests) throws IOException {
    var response = HTTP_FACTORY
      .buildPostRequest(
        new GenericUrl(this.endpoint.url() + "forward"),
        new ByteArrayContent("application/json", GSON.toJson(requests).getBytes(StandardCharsets.UTF_8)))
      .setParser(new JsonObjectParser(GSON_FACTORY))
      .execute();

    try (var reader = new InputStreamReader(response.getContent(), response.getContentCharset())) {
      return GSON
        .fromJson(reader, Broker.WorkloadResponse.class)
        .toEncryptedMessage();
    }
  }

  public int run() throws Exception {
    //
    // Get tokens from broker.
    //
    List<RequestToken> tokens = null;
    while (tokens == null || tokens.isEmpty()) {
      tokens = getTokens();
      if (tokens.isEmpty()) {
        System.err.println(
          "[INFO] Waiting for workload instances to become available...");

        try {
          Thread.sleep(5000);
        }
        catch (InterruptedException ignored) {
        }
      }
    }

    System.out.printf("Received %d tokens from broker\n", tokens.size());
    try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        //
        // Create a local, ephemeral key pair for encrypting messages. We use
        // each keypair for a single prompt only, that makes it more difficult
        // for the broker and workload to link two messages as coming from
        // the same client.
        //
        var keyPair = RequestEncryptionKeyPair.generate();

        //
        // Prompt for input.
        //
        System.out.print("Enter a question> ");
        var prompt = reader.readLine();
        if (prompt.isBlank()) {
          break;
        }

        //
        // Select a random subset of tokens and prepare a request for each.
        //
        var requests = new LinkedList<Broker.WorkloadRequest>();
        var instances = new LinkedList<String>();
        for (var token : tokens.stream()
          .sorted(Comparator.comparingDouble(x -> RANDOM.nextInt(32)))
          .limit(5)
          .toList()
        ) {
          //
          // Each token corresponds to a workload instance. Verify the token
          // and extract the public key of the workload instance.
          //
          var tokenPayload = token
            .attestationToken()
            .verify(this.endpoint.url(), !this.debug);

          //
          // Encrypt the prompt for the specific workload instance.
          //
          var message = new Message(prompt, keyPair.publicKey())
            .encrypt(tokenPayload.requestEncryptionKey());

          instances.add(tokenPayload.instanceName());
          requests.add(new Broker.WorkloadRequest(token, message));
        }

        System.out.printf("[INFO] Sending request to %s\n", String.join(", ", instances));

        //
        // Send the batch of requests to the broker.
        //
        var response = forward(requests);
        var clearTextResponse = response
          .decrypt(keyPair.privateKey())
          .toString();

        System.out.println(clearTextResponse);
        System.out.println();
      }
    }
    return 0;
  }
}
