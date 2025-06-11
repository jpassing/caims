package com.google.solutions.caims.client;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.solutions.caims.broker.RequestToken;
import com.google.solutions.caims.protocol.Message;
import com.google.solutions.caims.protocol.RequestEncryptionKeyPair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;

public class Client {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Gson GSON = new Gson();
  private static final HttpRequestFactory HTTP_FACTORY = new NetHttpTransport().createRequestFactory();
  private final @NotNull GenericUrl brokerUrl;
  private final boolean debug;

  public Client(@NotNull String brokerUrl, boolean debug) {
    this.brokerUrl = new GenericUrl(brokerUrl);
    this.debug = debug;
  }

  private List<RequestToken> getTokens() throws IOException {
    var response = HTTP_FACTORY
      .buildGetRequest(this.brokerUrl)
      .setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()))
      .execute();

    try (var reader = new InputStreamReader(response.getContent(), response.getContentCharset())) {
      return (List<RequestToken>)GSON.fromJson(reader, new TypeToken<List<RequestToken>>() {}.getType());
    }
  }

  public int run() throws IOException, GeneralSecurityException {
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
        // Randomly select one token.
        //
        System.out.println("Selecting a random token");
        var token = tokens.stream()
          .sorted(Comparator.comparingDouble(x -> RANDOM.nextInt(32)))
          .findFirst()
          .get();

        //token.attestationToken().verify()
        //
        // Wrap prompt in an encrypted message.
        //
        //new Message(prompt, keyPair.publicKey()).e

      }
    }
    return 0;
  }
}
