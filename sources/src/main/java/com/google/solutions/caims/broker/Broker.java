package com.google.solutions.caims.broker;

import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Broker {

  void refreshRegistrations(
    @NotNull Set<Registration> registrations
  ) {

  }

  /**
   * A registered node that is ready to handle requests.
   */
  public record Registration(
    @NotNull String projectId,
    @NotNull String zone,
    @NotNull String instanceName,
    @NotNull AttestationToken attestationToken
    ) {}
}
