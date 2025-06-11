package com.google.solutions.caims.workload;

import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import com.google.solutions.caims.protocol.RequestEncryptionKeyPair;
import org.jetbrains.annotations.NotNull;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A token attesting the configuration of the TEE and its VM.
 */
public record AttestationToken(@NotNull String token) {
  private static final String EXPECTED_ISSUER = "https://confidentialcomputing.googleapis.com";
  private static final String JWKS_URL = "https://www.googleapis.com/service_accounts/v1/metadata/jwk/signer@confidentialspace-sign.iam.gserviceaccount.com";

  /**
   * Verify the authenticity of an attestation token and extract its claims.
   */
  public Payload verify(
    @NotNull String expectedAudience,
    boolean requireProduction
  ) throws TokenVerifier.VerificationException {
    //
    // Perform a default verification on the JWT, which includes checking that
    // the token has been issued by the expected issuer, i.e. the Confidential
    // Space attestation povider.
    //
    var payload = new Payload(TokenVerifier
      .newBuilder()
      .setCertificatesLocation(JWKS_URL)
      .setIssuer(EXPECTED_ISSUER)
      .setAudience(expectedAudience)
      .build()
      .verify(this.token)
      .getPayload());

    if (requireProduction && !payload.isProduction()) {
      throw new TokenVerifier.VerificationException(
        "The attested node is in debug mode and can't be trusted");
    }

    return  payload;
  }

  public record Payload(@NotNull JsonWebToken.Payload jsonPayload) {
    private @NotNull String gceClaimValue(@NotNull String claim) {
      if (this.jsonPayload.get("submods") instanceof Map<?, ?> submods &&
        submods.get("gce") instanceof Map<?, ?> gce &&
        gce.get(claim) instanceof String instance) {
        return instance;
      }
      else {
        throw new IllegalStateException(
          String.format("The token does not contain a %s claim", claim));
      }
    }

    /**
     * Check if the attested node is in production mode. Debug nodes are
     * accessible to operators and therefore can't be trusted.
     */
    public boolean isProduction() {
      return this.jsonPayload.get("dbgstat")
        .equals("disabled-since-boot");
    }

    /**
     * Get the token's designated audience.
     */
    public @NotNull String audience() {
      return (String) this.jsonPayload.getAudience();
    }

    /**
     * Get the node's request encryption key.
     */
    public @NotNull RequestEncryptionKeyPair.PublicKey requestEncryptionKey(
    ) throws GeneralSecurityException, TokenVerifier.VerificationException {
      if (this.jsonPayload.get("eat_nonce") instanceof List<?> nonces) {
        return RequestEncryptionKeyPair.PublicKey.fromBase64(nonces
          .stream()
          .map(Object::toString)
          .collect(Collectors.joining("")));
      }
      else {
        throw new TokenVerifier.VerificationException(
          "The attestation token does not contain a request encryption key");
      }
    }

    /**
     * Get the name of the attested instance.
     */
    public @NotNull String instanceName() {
      return gceClaimValue("instance_name");
    }

    /**
     * Get the zone the attested instance resides in.
     */
    public @NotNull String instanceZone() {
      return gceClaimValue("zone");
    }

    /**
     * Get the project the attested instance resides in.
     */
    public @NotNull String projectId() {
      return gceClaimValue("project_id");
    }
  }
}
