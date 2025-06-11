package com.google.solutions.caims.broker;

import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;

/**
 * Token that entitles a client to perform a request to a specific workload instance.
 *
 * If we wanted to customize the lifetime of request tokens or implement quota charging,
 * we could implement the request token as a custom JWT that might contain the following:
 *
 * <ul>
 *   <li>An identifier for the workload instance (such as the instance name)</li>
 *   <li>A JWT ID to enforce one-time-use semantics</li>
 * </ul>
 *
 * To keep the implementation simple, we're not using a custom JWT here and instead
 * use tha attestation token as request token:
 *
 * <ul>
 *   <li>The attestation token is also a JWT, and we can verify it</li>
 *   <li>The client needs to "see" the full attestation token anyway so that it can convince
 *   itself of the integrity of the workload, so we can just as well use it for this purpose too.
 *   </li>
 * </ul>>
 *
 * @param attestationToken attestation token of the workload instance.
 */
public record RequestToken(
  @NotNull AttestationToken attestationToken
  ) {

  public RequestToken(@NotNull String token) {
    this(new AttestationToken(token));
  }
}
