package com.google.solutions.caims.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * A message that has been encrypted by a sender and can only be decrypted by
 * the intended receiver.
 * <p>
 * If the message represents a request, then the sender is the client and
 * the recipient is the server. If the message represents a response, the
 * roles are reversed.
 *
 * @param cipherText Message body, encrypted.
 */
public record EncryptedMessage(byte[] cipherText) {

  /**
   * Get raw cipher text.
   */
  @Override
  public byte[] cipherText() {
    return cipherText;
  }

  /**
   * Decrypt the message using the recipient's private key.
   */
  public @NotNull Message decrypt(
    @NotNull RequestEncryptionKeyPair.PrivateKey recipientPrivateKey
  ) throws GeneralSecurityException, IOException {
    var clearText = recipientPrivateKey.decrypt(this.cipherText);

    try (var stream = new DataInputStream(new ByteArrayInputStream(clearText))) {
      var body = stream.readUTF();

      var senderPublicKey = stream.available() > 0
        ? RequestEncryptionKeyPair.PublicKey.read(stream)
        : null;

      return new Message(body, senderPublicKey);
    }
  }
}
