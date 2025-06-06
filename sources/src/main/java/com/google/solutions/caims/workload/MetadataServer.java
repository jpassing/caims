package com.google.solutions.caims.workload;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for interacting with the Compute Engine
 * instance metadata server.
 */
public class MetadataServer {
  private static final String BASE_URL = "http://169.254.169.254/computeMetadata/v1/instance/guest-attributes/";

  /**
   * Publish a guest attribute for the current VM.
   */
  public void setGuestAttribute(
    @NotNull String namespace,
    @NotNull String name,
    @NotNull String value
  ) throws IOException {
    Preconditions.checkArgument(!namespace.contains("/"), "Namespace must not contain slashes");
    Preconditions.checkArgument(!name.contains("/"), "Name must not contain slashes");

    var url = new URL(BASE_URL + namespace + "/" + name);

    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod("PUT");
      connection.setRequestProperty("Metadata-Flavor", "Google");
      connection.setDoOutput(true);

      //
      // Write the request.
      //
      byte[] postData = value.getBytes(StandardCharsets.UTF_8);
      connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
      try (var requestStream = connection.getOutputStream()) {
        requestStream.write(postData);
      }

      //
      // Read response.
      //
      int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new IOException(
          String.format("Setting guest attribute failed with HTTP error code: %d", responseCode));
      }
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
