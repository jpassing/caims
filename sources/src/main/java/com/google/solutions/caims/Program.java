package com.google.solutions.caims;

import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.solutions.caims.workload.ConfidentialSpace;
import com.google.solutions.caims.workload.MetadataServer;
import com.google.solutions.caims.workload.RegistrationDaemon;
import com.google.solutions.caims.workload.WorkloadServer;

import java.security.GeneralSecurityException;
import java.util.Arrays;

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
      case "client":
        System.out.println("Running as client");
        return;

      case "broker":
        System.out.println("Running as broker");
        return;

      case "workload":
        System.out.println("Running as workload");
        var server = new WorkloadServer(8080, 10);
        var daemon = new RegistrationDaemon(
          server,
          new ConfidentialSpace(),
          new MetadataServer());

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