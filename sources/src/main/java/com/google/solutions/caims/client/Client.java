package com.google.solutions.caims.client;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.solutions.caims.broker.Broker;
import com.google.solutions.caims.broker.RequestToken;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.protocol.Message;
import com.google.solutions.caims.protocol.RequestEncryptionKeyPair;
import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      .setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()))
      .execute();

    try (var reader = new InputStreamReader(response.getContent(), response.getContentCharset())) {
      return (List<RequestToken>)GSON.fromJson(reader, new TypeToken<List<RequestToken>>() {}.getType());
    }
  }

  private EncryptedMessage forward(Map<RequestToken, EncryptedMessage> request) throws IOException {
    var response = HTTP_FACTORY
      .buildPostRequest(
        new GenericUrl(this.endpoint.url() + "forward"),
        new JsonHttpContent(GSON_FACTORY, request))
      .execute();

    try (var stream = new DataInputStream(response.getContent())) {
      return EncryptedMessage.read(stream);
    }
  }

  public int run() throws IOException, GeneralSecurityException, TokenVerifier.VerificationException {
    //
    // Get tokens from broker.
    //
    var tokens = getTokens();

    if (tokens.isEmpty()) {
      System.err.println(
        "The broker did not return any tokens. This indicates that there is " +
        "no workload instance available to serve inference requests.");
      return 1;
    }

    System.out.printf("Received %d tokens from broker\n", tokens.size());

    var keyPair = RequestEncryptionKeyPair.generate();

    try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        //
        // Prompt for input.
        //
        System.out.print("> ");
        var prompt = reader.readLine();
        if (prompt.isBlank()) {
          break;
        }

        //
        // Select a random subset of tokens and prepare a request for each.
        //
        System.out.println("[INFO] Selecting a random subset of tokens");

        var request = new HashMap<RequestToken, EncryptedMessage>();
        for (var token : tokens.stream()
          .sorted(Comparator.comparingDouble(x -> RANDOM.nextInt(32)))
          .limit(5)
          .toList()
        ) {
          //
          // Each token corresponds to a workload instance. Verify the token
          // and extract the public key of the workload instance.
          //
          var requestEncryptionKey = token
            .attestationToken()
            .verify(this.endpoint.url(), !this.debug)
            .requestEncryptionKey();

          //
          // Encrypt the prompt for the specific workload instance.
          //
          request.put(
            token,
            new Message(prompt, keyPair.publicKey()).encrypt(requestEncryptionKey));
        }

        var clearTextResponse = this
          .forward(request)
          .decrypt(keyPair.privateKey())
          .toString();

        System.out.println(clearTextResponse);
        System.out.println();
      }
    }
    return 0;
  }
}
