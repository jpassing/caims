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
  /**
   * Message body, encrypted.
   */
  private final @NotNull byte[] cipherText;

  EncryptedMessage(@NotNull byte[] cipherText) {
    this.cipherText = cipherText;
  }

  /**
   * Read encrypted message from a stream.
   */
  public static @NotNull EncryptedMessage read(
    @NotNull DataInputStream stream,
    int maxSize
  ) throws IOException {
    var length = stream.readInt();
    if (length == 0 || length > maxSize) {
      throw new IOException("The stream does not contain a valid message");
    }

    return new EncryptedMessage(stream.readNBytes(length));
  }

  /**
   * Write encrypted message to a stream.
   */
  public void write(
    @NotNull DataOutputStream stream
  ) throws IOException {
    stream.writeInt(this.cipherText.length);
    stream.write(this.cipherText);
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
