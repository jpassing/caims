package com.google.solutions.caims.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

/**
 * A clear-text message that is ready to be encrypted for a particular
 * recipient.
 *
 * If the message represents a request, then the sender is the client and
 * the recipient is the server. If the message represents a response, the
 * roles are reversed.
 *
 * Note that HPKE encryption is unidirectional from sender to recipient.
 * To allow the recipient to return an encrypted response, HPKE allows
 * the recipient to export a derived key (see RFC9180 5.3.). However, this
 * export functionality isn't implemented in Tink-Java, so we don't use it here.
 *
 * Instead, we re-initiate HPKE for the return path using the client's
 * public key. This key is ephemeral and passed to the recipient as
 * associated-data.
 */
public class Message {
  /** Message body, in clear text. */
  private final @NotNull String body;

  /**
   * Public key of the sender, only relevant if the message denotes
   * a request for which the sender expects an encrypted response.
   */
  private final @Nullable RequestEncryptionKeyPair.PublicKey senderPublicKey;

  public Message(
    @NotNull String body,
    @Nullable RequestEncryptionKeyPair.PublicKey senderPublicKey
  ) {
    this.body = body;
    this.senderPublicKey = senderPublicKey;
  }

  @Override
  public String toString() {
    return this.body;
  }

  /**
   * Get the sender's public key.
   */
  public @NotNull Optional<RequestEncryptionKeyPair.PublicKey> senderPublicKey() {
    return Optional.ofNullable(senderPublicKey);
  }

  /**
   * Encrypt this message using the recipient's public key.
   */
  public @NotNull EncryptedMessage encrypt(
    @NotNull RequestEncryptionKeyPair.PublicKey recipientPublicKey
  ) throws GeneralSecurityException, IOException {
    try (var buffer = new ByteArrayOutputStream())
    {
      try (var stream = new DataOutputStream(buffer)) {
        stream.writeUTF(this.body);

        //
        // If this message denotes a request, then we need to make sure that
        // the recipient gets the sender's public key so that it can use that
        // to encrypt the response message.
        //
        if (this.senderPublicKey != null) {
          this.senderPublicKey.write(stream);
        }
      }

      var cipherText = recipientPublicKey.encrypt(buffer.toByteArray());
      return new EncryptedMessage(cipherText);
    }
  }
}
