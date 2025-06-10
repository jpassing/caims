package com.google.solutions.caims.workload;

import com.google.solutions.caims.broker.Broker;
import org.jetbrains.annotations.NotNull;

/**
 * Daemon that registers the workload server.
 */
public class RegistrationDaemon extends Thread {
  private static final int MINUTES = 60 * 1000;
  public static final String GUEST_ATTRIBUTE_NAMESPACE = "workload-server";
  public static final String GUEST_ATTRIBUTE_NAME = "token";

  private final @NotNull Broker.Identifier brokerId;
  private final @NotNull Workload server;
  private final @NotNull ConfidentialSpace confidentialSpace;
  private final @NotNull MetadataClient metadataClient;


  public RegistrationDaemon(
    @NotNull Broker.Identifier brokerId,
    @NotNull Workload server,
    @NotNull ConfidentialSpace confidentialSpace,
    @NotNull MetadataClient metadataClient
  ) {
    this.brokerId = brokerId;
    this.server = server;
    this.confidentialSpace = confidentialSpace;
    this.metadataClient = metadataClient;

    setDaemon(true);
  }

  @Override
  public void run() {
    //
    // Register the server and keep refreshing the registration
    // every so often. The refresh is necessary because the attestation
    // token has a finite lifetime.
    //
    while (true) {
      System.out.println("[INFO] Refreshing server registration...");

      try {
        //
        // Get a fresh attestation tokens and publish it as guest attribute
        // so that the broker can discover and use the workload server.
        //
        var attestationToken = this.confidentialSpace.getAttestationToken(
          this.brokerId.toString(),
          this.server.publicKey());

        this.metadataClient.setGuestAttribute(
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
