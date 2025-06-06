package com.google.solutions.caims.workload;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Daemon that registers the workload server
 */
public class RegistrationDaemon extends Thread {
  private static final int MINUTES = 60 * 1000;
  public static final String GUEST_ATTRIBUTE_NAMESPACE = "workload-server";
  public static final String GUEST_ATTRIBUTE_NAME = "token";

  private final @NotNull WorkloadServer server;
  private final @NotNull ConfidentialSpace confidentialSpace;
  private final @NotNull MetadataServer metadataServer;


  public RegistrationDaemon(
    @NotNull WorkloadServer server,
    @NotNull ConfidentialSpace confidentialSpace,
    @NotNull MetadataServer metadataServer
  ) {
    this.server = server;
    this.confidentialSpace = confidentialSpace;
    this.metadataServer = metadataServer;
  }

  @Override
  public void run() {
    //
    // Register the server and keep refreshing the registration
    // every so often. The refresh is necessary because the attestation
    // token has a finite lifetime.
    //
    while (true)
    {
      System.out.println("[INFO] Refreshing server registration...");

      try {
        //
        // Get a fresh attestation tokens and publish it as guest attribute
        // so that the broker can discover and use the workload server.
        //
        var attestationToken = this.confidentialSpace.getAttestationToken(
          "http://broker.example.com",
          List.<String>of());

        this.metadataServer.setGuestAttribute(
          GUEST_ATTRIBUTE_NAMESPACE,
          GUEST_ATTRIBUTE_NAME,
          attestationToken.token());

      }
      catch (Exception e) {
        System.err.printf("[ERROR] Server registration failed: %s\n", e.getMessage());
        e.printStackTrace();
      }

      try {
        Thread.sleep(5 * MINUTES);
      }
      catch (InterruptedException ignored) {
      }
    }
  }
}
