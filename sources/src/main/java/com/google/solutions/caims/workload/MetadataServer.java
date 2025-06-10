package com.google.solutions.caims.workload;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.GenericData;
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
import java.nio.charset.StandardCharsets;

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

    var url = new GenericUrl(String.format(
      "%s/computeMetadata/v1/instance/guest-attributes/%s/%s",
      ComputeEngineCredentials.getMetadataServerUrl(),
      namespace,
      name));

    var request = new NetHttpTransport()
      .createRequestFactory()
      .buildPutRequest(
        url,
        new ByteArrayContent("text/plain", value.getBytes(StandardCharsets.UTF_8)));

    request.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
    request.getHeaders().set("Metadata-Flavor", "Google");
    request.setThrowExceptionOnExecuteError(true);

    try {
      request.execute();
    }
    catch (Exception exception) {
      throw new IOException(
        "Setting guest attribute failed",
        exception);
    }
  }

  /**
   * Get instance metadata for the current VM.
   */
  public @NotNull GenericData getInstanceMetadata() throws IOException {
    var url = new GenericUrl(
      ComputeEngineCredentials.getMetadataServerUrl() +
        "/computeMetadata/v1/project/?recursive=true");

    var request = new NetHttpTransport()
      .createRequestFactory()
      .buildGetRequest(url);

    request.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
    request.getHeaders().set("Metadata-Flavor", "Google");
    request.setThrowExceptionOnExecuteError(true);

    try {
      return request
        .execute()
        .parseAs(GenericData.class);
    }
    catch (Exception exception) {
      throw new IOException(
        "Cannot find the metadata server, possibly because code is not running on Google Cloud",
        exception);
    }
  }
}
