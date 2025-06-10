package com.google.solutions.caims.broker;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.compute.Compute;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.solutions.caims.UserAgent;
import com.google.solutions.caims.workload.AttestationToken;
import com.google.solutions.caims.workload.RegistrationDaemon;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Optional;

/**
 * Daemon that discovers workload servers.
 */
public class DiscoveryDaemon extends Thread {
  private static final int MINUTES = 60 * 1000;
  private  final @NotNull Broker broker;
  private final @NotNull Compute computeClient;
  private final @NotNull String projectId;

  public DiscoveryDaemon(
    @NotNull Broker broker,
    @NotNull GoogleCredentials credentials,
    @NotNull String projectId
  ) throws GeneralSecurityException, IOException {
    this.broker = broker;
    this.projectId = projectId;

    this.computeClient = new Compute.Builder(
      GoogleNetHttpTransport.newTrustedTransport(),
      new GsonFactory(),
      new HttpCredentialsAdapter(credentials))
      .setApplicationName(UserAgent.VALUE)
      .build();

    setDaemon(true);
  }

  @Override
  public void run() {
    while (true) {
      try {
        //
        // Find all confidential space instances in the project.
        //
        var instances = this.computeClient.instances()
          .aggregatedList(this.projectId)
          .setReturnPartialSuccess(true)
          .setFilter("status=\"RUNNING\"")
          .execute();

        var confidentialSpaceInstances = instances.getItems()
          .values()
          .stream()
          .flatMap(i -> Optional.ofNullable(i.getInstances()).stream())
          .flatMap(i -> i.stream())
          .filter(i -> i.getMetadata().containsKey("tee-image-reference"))
          .toList();

        var registrations = new HashSet<Broker.Registration>();
        for (var instance : confidentialSpaceInstances) {
          //
          // Read the instance's attestation token. This might fail if the instance
          // hasn't finished initializing yet or if it's an unrelated instance.
          //
          try {
            var attestationToken = new AttestationToken(this.computeClient
              .instances()
              .getGuestAttributes(this.projectId, instance.getZone(), instance.getName())
              .setQueryPath(
                String.format(
                  "%s/%s",
                  RegistrationDaemon.GUEST_ATTRIBUTE_NAMESPACE,
                  RegistrationDaemon.GUEST_ATTRIBUTE_NAME))
              .execute()
              .getQueryValue()
              .getItems()
              .stream()
              .findFirst()
              .map(v -> v.getValue())
              .get());

            registrations.add(new Broker.Registration(
              this.projectId,
              instance.getZone(),
              instance.getName(),
              attestationToken));
          }
          catch (Exception e) {
            System.err.printf(
              "[INFO] Ignoring instance %s because it has not registered an attestation token (%s)\n",
              instance.getName(),
              e.getMessage());
          }
        }

        //
        // We've scanned all relevant instance and have an up-to-date view on
        // which instances are available.
        //
        this.broker.refreshRegistrations(registrations);
      }
      catch (IOException e) {
        System.err.printf("[ERROR] Instance discovery failed: %s\n", e.getMessage());
        e.printStackTrace();
      }

      try {
        Thread.sleep(1 * MINUTES);
      }
      catch (InterruptedException ignored) {
      }
    }
  }
}
