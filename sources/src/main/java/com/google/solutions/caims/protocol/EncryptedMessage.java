package com.google.solutions.caims.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * An encrypted message that can only be decrypted by the intended
 * recipient.
 */
public class EncryptedMessage {

  /** Maximum allowed size for a message */
  private static final int MAX_SIZE = 1024;

  /**
   * Message body, encrypted.
   */
  private final @NotNull byte[] cipherText;

  public EncryptedMessage(@NotNull byte[] cipherText) {
    this.cipherText = cipherText;
  }

  /**
   * Get raw cipher text.
   */
  public byte[] cipherText() {
    return cipherText;
  }

  public @NotNull Message decrypt(
    @NotNull RequestEncryptionKeyPair.PrivateKey recipientPrivateKey
  ) throws GeneralSecurityException, IOException {
    var clearText = recipientPrivateKey.decrypt(this.cipherText);

    try (var stream = new DataInputStream(new ByteArrayInputStream(clearText)) ){
      var body = stream.readUTF();

      var senderPublicKey = stream.available() > 0
        ? RequestEncryptionKeyPair.PublicKey.read(stream)
        : null;

      return new Message(body, senderPublicKey);
    }
  }
}
