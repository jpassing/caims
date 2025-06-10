package com.google.solutions.caims;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.solutions.caims.broker.Broker;
import com.google.solutions.caims.broker.DiscoveryDaemon;
import com.google.solutions.caims.workload.ConfidentialSpace;
import com.google.solutions.caims.workload.MetadataClient;
import com.google.solutions.caims.workload.RegistrationDaemon;
import com.google.solutions.caims.workload.Workload;

import java.security.GeneralSecurityException;
import java.util.Arrays;
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

    var metadataClient = new MetadataClient();

    switch (action) {
      case "client":
        //
        // Run the client (on a local workstation). The client simulates a
        // front-end app (or phone app) which an end-user interacts with.
        //
        System.out.println("[INFO] Running as client");
        return;

      case "broker":
        //
        // Run the broker (on Cloud Run). The broker helps the client find
        // available workload instances and forwards E2E-encrypted
        // inference requests from the client to workload instances.
        //
        System.out.println("[INFO] Running as broker");
        var broker = new Broker(
          Optional
            .ofNullable(System.getenv("PORT"))
            .map(Integer::parseInt)
            .orElse(8080),
          10);
        var discoveryDaemon = new DiscoveryDaemon(
          broker,
          GoogleCredentials.getApplicationDefault(),
          (String)metadataClient.getInstanceMetadata().get("projectId"));

        discoveryDaemon.start();
        broker.start();
        return;

      case "workload":
        //
        // Run the workload (in a trusted execution environment). The workload
        // receives E2E-encrypted inference requests from the broker and evaluates
        // them.
        //
        System.out.println("[INFO] Running as workload");
        var server = new Workload(8080, 10);
        var daemon = new RegistrationDaemon(
          server,
          new ConfidentialSpace(),
          metadataClient);

        daemon.start();
        server.start();

        return;

      default:
        System.err.printf("Unrecognized action '%s'\n", action);
        System.err.println("Supported actions are:");
        System.err.println("  client:    Run client application");
        System.err.println("  broker:    Run broker (typically run in Cloud Run)");
        System.err.println("  workload:  Run workload server (typically run on a confidential VM)");
    }
  }
}