package com.google.solutions.caims;

import com.google.crypto.tink.hybrid.HybridConfig;

import java.io.IOException;
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

      case "server":
        System.out.println("Running as server");
        new Server(8080, 10).start();
        return;

      default:
        System.err.printf("Unrecognized action '%s'\n", action);
        System.err.println("Supported actions are:");
        System.err.println("  client:    Run client application");
        System.err.println("  broker:    Run broker (typically run in Cloud Run)");
        System.err.println("  server:    Run server (typically run on a confidential VM)");
    }
  }
}