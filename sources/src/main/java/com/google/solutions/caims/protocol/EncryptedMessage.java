package com.google.solutions.caims.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An encrypted message that can only be decrypted by the intended
 * recipient.
 */
public class EncryptedMessage {
  /**
   * Message body, encrypted.
   */
  private final @NotNull byte[] body;

  EncryptedMessage(@NotNull byte[] body) {
    this.body = body;
  }

  /**
   * Read encrypted request from a raw stream.
   */
  public static @NotNull Message read(
    @NotNull InputStream stream,
    int maxSize
  ) {
    throw new RuntimeException("NIY");
  }

  public void write(@NotNull OutputStream stream) {
    throw new RuntimeException("NIY");
  }

  public @NotNull Message decrypt(
    @NotNull RequestEncryptionKeyPair.PrivateKey recipientPrivateKey
  ) {
    throw new RuntimeException("NIY");
  }
}
