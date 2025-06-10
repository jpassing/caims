package com.google.solutions.caims.workload;

import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Helper class for interacting with the Compute Engine
 * instance metadata server.
 */
public class MetadataServer {
  private static final HttpClient client = HttpClient
    .newBuilder()
    .build();

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

    try {
      var request = HttpRequest
        .newBuilder()
        .uri(new URI(
          String.format(
            "%s/computeMetadata/v1/instance/guest-attributes/%s/%s",
            ComputeEngineCredentials.getMetadataServerUrl(),
            namespace,
            name)))
        .header("Metadata-Flavor", "Google")
        .PUT(HttpRequest.BodyPublishers.ofString(value))
        .build();

      var response = this.client
        .send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException(
          String.format(
            "Setting guest attribute failed with HTTP error code: %d",
            response.statusCode()));
      }
    }
    catch (URISyntaxException | InterruptedException e) {
      throw new IOException(e);
    }
  }
}
