package com.google.solutions.caims.workload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfidentialSpace {
  /** Path to the Unix domain socket of the trusted execution environment server */
  private static final String TEE_SERVER_SOCKET_PATH = "/run/container_launcher/teeserver.sock";

  /** Endpoint of the token endpoint */
  private static final String TEE_TOKEN_ENDPOINT = "/v1/token";

  private static final Gson GSON = new GsonBuilder().create();

  /**
   * Request an attestation token from the TEE server. This method only works
   * when used inside a Confidential Space trusted execution environment.
   */
  public @NotNull AttestationToken getAttestationToken(
    @NotNull String audience,
    @NotNull List<String> nonces
    ) throws IOException {
    if (!new File(TEE_SERVER_SOCKET_PATH).exists()) {
      throw new ConfidentialSpaceException(
        String.format("TEE socket not found at %s, the most likely reason for " +
          "that is that the workload server is executed outside a confidential" +
          "space TEE", TEE_SERVER_SOCKET_PATH));
    }
    //
    // Java's built-in HTTP client doesn't support sending HTTP requests over a
    // Unix domain socket, so we need to construct the request manually.
    //
    var address = UnixDomainSocketAddress.of(TEE_SERVER_SOCKET_PATH);

    var requestBody = new HashMap<String, Object>();
    requestBody.put("audience", audience);
    requestBody.put("token_type", "OIDC");
    requestBody.put("nonces", nonces);

    try (var clientChannel = SocketChannel.open(address)) {
      //
      // Format a HTTP request.
      //
      var httpRequest = String.format("POST %s HTTP/1.1\r\n" +
        "Host: localhost\r\n" +
        "Connection: close\r\n" +
        "Content-type: application/json\r\n" +
        "\r\n" +
        "%s\r\n",
        TEE_TOKEN_ENDPOINT,
        GSON.toJson(requestBody));

      //
      // Write request to the channel.
      //
      writeString(clientChannel, httpRequest);

      //
      // Read response from channel.
      //
      var httpResponse = readString(clientChannel);

      //
      // Validate response and extract the response body.
      //
      if (!httpResponse.startsWith("HTTP 200 OK")) {
        System.err.printf("[ERROR] Received unexpected response from TEE server: %s\n", httpResponse);
        throw new ConfidentialSpaceException("Received unexpected response from TEE server");
      }

      var body = Arrays.stream(httpResponse.split("\r\n\r\n"))
        .skip(1)
        .findFirst();
      if (!body.isPresent() || !body.get().startsWith("ey")) {
        System.err.printf("[ERROR] Received empty or unexpected response from TEE server: %s\n", httpResponse);
        throw new ConfidentialSpaceException("Received empty or unexpected response from TEE server");
      }

      return new AttestationToken(body.get().trim());
    }
  }

  private static void writeString(
    @NotNull SocketChannel channel,
    @NotNull String s
  ) throws IOException {
    var buffer = ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));

    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }

  private static @NotNull String readString(
    @NotNull SocketChannel channel
  ) throws IOException {
    var buffer = ByteBuffer.allocate(1024);
    var result = new StringBuilder();

    while (channel.read(buffer) != -1) {
      buffer.flip();
      result.append(StandardCharsets.UTF_8.decode(buffer));
      buffer.clear();
    }

    return result.toString();
  }

  /**
   * A token attesting the configuration of the TEE and its VM.
   */
  public record AttestationToken(@NotNull String token) {}
}
