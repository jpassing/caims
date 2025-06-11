package com.google.solutions.caims;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.reflect.TypeToken;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.solutions.caims.broker.Broker;
import com.google.solutions.caims.broker.DiscoveryDaemon;
import com.google.solutions.caims.broker.RequestToken;
import com.google.solutions.caims.workload.ConfidentialSpace;
import com.google.solutions.caims.workload.MetadataClient;
import com.google.solutions.caims.workload.RegistrationDaemon;
import com.google.solutions.caims.workload.Workload;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Program {
  static {
    //
    // Initialize Tink so that we can use HPKE.
    //
    try {
      HybridConfig.register();
    }
    catch (GeneralSecurityException e)
    {
      throw new RuntimeException("Initializing Tink failed", e);
    }
  }

  /**
   * Entry point, typically invoked using maven or by running
   * <c>java -jar</c>.
   */
  public static void main(String[] args) throws Exception {
    var action = Arrays.stream(args).findFirst().orElse("");

    switch (action) {
      case "client": {
        System.exit(runClient(Arrays.stream(args).skip(1).toList()));
      }
      case "broker": {
        System.exit(runBroker());
      }
      case "workload": {
        System.exit(runWorkload());
      }
      default:
        System.exit(showUsageInformation());
    }
  }

  /**
   * Show usage information for the command line app.
   */
  private static int showUsageInformation() {
    System.err.println("Supported actions are:");
    System.err.println("  client:    Run client application");
    System.err.println("     ---broker URL   URL of broker to use (required)");
    System.err.println("     ---debug        Allow debug workloads (optional)");
    System.err.println("  broker:    Run broker (typically run in Cloud Run)");
    System.err.println("  workload:  Run workload server (typically run on a confidential VM)");
    return 1;
  }

  /**
   * Run the workload (in a trusted execution environment). The workload
   * receives E2E-encrypted inference requests from the broker and evaluates
   * them.
   */
  private static int runWorkload() throws IOException, GeneralSecurityException {
    System.out.println("[INFO] Running as workload");

    var metadataClient = new MetadataClient();
    var metadata = metadataClient.getInstanceMetadata();

    var server = new Workload(8080, 10);
    var daemon = new RegistrationDaemon(
      new Broker.Identifier(metadata.get("numericProjectId").toString()),
      server,
      new ConfidentialSpace(),
      metadataClient);

    daemon.start();
    server.start();

    return 0;
  }

  /**
   * Run the broker (on Cloud Run). The broker helps the client find
   * available workload instances and forwards E2E-encrypted
   * inference requests from the client to workload instances.
   */
  private static int runBroker() throws IOException, GeneralSecurityException {
    System.out.println("[INFO] Running as broker");

    var metadata = new GenericJson()
      .set("projectId", "jpassing-ar-cs-1")
      .set("numericProjectId", "1"); // TODO: add switch
    //var metadata = metadataClient.getInstanceMetadata();

    var broker = new Broker(
      new Broker.Identifier(metadata.get("numericProjectId")
        .toString()),
      Optional
        .ofNullable(System.getenv("PORT"))
        .map(Integer::parseInt)
        .orElse(8080),
      10,
      10);
    var discoveryDaemon = new DiscoveryDaemon(
      broker,
      GoogleCredentials.getApplicationDefault(),
      metadata.get("projectId").toString());

    discoveryDaemon.start();
    broker.start();

    return 0;
  }

  /**
   * Run the client (on a local workstation). The client simulates a
   * front-end app (or phone app) which an end-user interacts with.
   */
  private static int runClient(List<String> args) throws IOException {
    System.out.println("[INFO] Running as client");

    //
    // Parse command line arguments.
    //
    boolean debug = false;
    String brokerUrl = null;
    for (int i = 0; i < args.size(); i++) {
      if ("--debug".equals(args.get(i))) {
        debug = true;
      }
      else if ("--broker".equals(args.get(i)) && i < args.size() - 1) {
       brokerUrl = args.get(i + i);
      }
      else {
        return showUsageInformation();
      }
    }

    if (brokerUrl == null) {
      return showUsageInformation();
    }

    //
    // Get tokens from broker.
    //
    var tokens = (List<RequestToken>)new NetHttpTransport()
      .createRequestFactory()
      .buildGetRequest(new GenericUrl(brokerUrl))
      .setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()))
      .execute()
      .parseAs(new TypeToken<List<RequestToken>>() {}.getType());

    System.out.printf("Received %d tokens from broker", tokens.size());
    
    return 0;
  }
}