package com.google.solutions;

import java.io.IOException;
import java.util.Arrays;

public class Program {
  /**
   * Entry point, typically invoked using maven or by running
   * <c>java -jar</c>.
   */
  public static void main(String[] args) throws IOException {
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
        new Server(8080).start();
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