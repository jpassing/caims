package com.google.solutions.caims.protocol;

import com.google.crypto.tink.*;
import com.google.crypto.tink.hybrid.HybridKeyTemplates;
import com.google.crypto.tink.proto.KeyTemplate;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.GeneralSecurityException;

/**
 * A (typically ephemeral) key pair for encrypting requests.
 */
public class RequestEncryptionKeyPair {
  /**
   * Key type to use for hybrid encryption.
   *
   * Key encapsulation (KEM): Diffie-Hellman using P-256 curve
   * Key Derivation (KDF): HMAC-SHA256
   * Authenticated Encryption with Associated Data (AEAD): AES-128 GCM
   */
  private static @NotNull KeyTemplate TEMPLATE
    = HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM;

  private final @NotNull PublicKey publicKey;
  private final @NotNull PrivateKey privateKey;

  /**
   * Get the public key part of te key pair.
   */
  public @NotNull PublicKey publicKey() {
    return publicKey;
  }

  /**
   * Get the private key part of te key pair.
   */
  public @NotNull PrivateKey privateKey() {
    return privateKey;
  }

  //---------------------------------------------------------------------------
  // Constructor and factory methods.
  //---------------------------------------------------------------------------

  private RequestEncryptionKeyPair(
    @NotNull PrivateKey privateKey,
    @NotNull PublicKey publicKey
  ) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  /**
   * Generate a new key pair.
   */
  public static @NotNull RequestEncryptionKeyPair generate() throws GeneralSecurityException {
    var handle = KeysetHandle.generateNew(TEMPLATE);

    return new RequestEncryptionKeyPair(
      new PrivateKey(handle),
      new PublicKey(handle.getPublicKeysetHandle()));
  }

  //---------------------------------------------------------------------------
  // Inner classes.
  //---------------------------------------------------------------------------

  /**
   * Public portion of the key pair.
   */
  public static class PublicKey {
    private final @NotNull KeysetHandle handle;

    /**
     * Get the key's parameters.
     */
    Parameters parameters() {
      return this.handle.getPrimary().getKey().getParameters();
    }

    private PublicKey(@NotNull KeysetHandle handle) {
      this.handle = handle;
    }

    /**
     * Use the public key to encrypt a piece of clear text and,
     * optionally a piece of associated data.
     */
    public @NotNull byte[] encrypt(
      @NotNull byte[] clearText
    ) throws GeneralSecurityException {
      return this.handle
        .getPrimitive(RegistryConfiguration.get(), HybridEncrypt.class)
        .encrypt(clearText, null);
    }

    /**
     * Serialize key using Tink's native format and write it to a stream.
     */
    static @NotNull PublicKey read(
      @NotNull DataInputStream stream
    ) throws GeneralSecurityException, IOException {
      var size = stream.readInt();
      if (size == 0) {
        throw new IOException("The stream does not contain a valid key");
      }

      var keySet = TinkProtoKeysetFormat.parseKeysetWithoutSecret(stream.readNBytes(size));
      return new PublicKey(keySet);
    }

    /**
     * Read a serialized key from a stream.
     */
    void write(
      @NotNull DataOutputStream stream
    ) throws GeneralSecurityException, IOException {
      var serialized = TinkProtoKeysetFormat.serializeKeysetWithoutSecret(this.handle);
      stream.writeInt(serialized.length);
      stream.write(serialized);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof PublicKey other &&
        this.handle.equalsKeyset(other.handle);
    }
  }

  /**
   * Private portion of the key pair.
   */
  public static class PrivateKey {
    private final @NotNull KeysetHandle handle;

    private PrivateKey(@NotNull KeysetHandle handle) {
      this.handle = handle;
    }

    /**
     * Use the private key to decrypt a piece of clear text and,
     * optionally a piece of associated data.
     */
    public @NotNull  byte[] decrypt(
      @NotNull byte[] cipherText
    ) throws GeneralSecurityException {
      return this.handle
        .getPrimitive(RegistryConfiguration.get(), HybridDecrypt.class)
        .decrypt(cipherText, null);
    }
  }
}
