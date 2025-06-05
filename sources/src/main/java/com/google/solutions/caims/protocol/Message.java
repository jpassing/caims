package com.google.solutions.caims.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * A clear-text message that is ready to be encrypted for a particular
 * recipient.
 *
 * If the message denotes a request, then the sender is the client and
 * the recipient is the server. If the message denotes a response, the
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
  /**
   * Message body, in clear text.
   */
  private final @NotNull byte[] body;

  /**
   * Public key of the sender, only relevant if the message denotes
   * a request for which the sender expects an encrypted response.
   */
  private final @Nullable RequestEncryptionKeyPair.PublicKey senderPublicKey;

  public Message(
    @NotNull byte[] body,
    @Nullable RequestEncryptionKeyPair.PublicKey senderPublicKey
  ) {
    this.body = body;
    this.senderPublicKey = senderPublicKey;
  }

  public Message(
    @NotNull String body,
    @Nullable RequestEncryptionKeyPair.PublicKey senderPublicKey
  ) {
    this(StandardCharsets.UTF_8.encode(body).array(), senderPublicKey);
  }

  public RequestEncryptionKeyPair.PublicKey senderPublicKey() {
    return senderPublicKey;
  }

  /**
   * Encrypt this message using the recipient's public key.
   */
  public @NotNull EncryptedMessage encrypt(
    @NotNull RequestEncryptionKeyPair.PublicKey recipientPublicKey
  ) throws GeneralSecurityException {
    //
    // If this message denotes a request, then we need to make sure that
    // the recipient gets the sender's public key so that it can use that
    // to encrypt the response message.
    //
    // There are two ways we could pass the public key:
    //
    // 1. as part of the body (i.e., encrypted)
    // 2. as associated data (i.e., clear-text but authenticated)
    //
    // Because the public key isn't confidential, option (2) is good
    // enough, so that's what we use here.
    //

    var serializedSenderKey = this.senderPublicKey == null
      ? null
      : this.senderPublicKey.toBinaryFormat();

    return new EncryptedMessage(
      recipientPublicKey.encrypt(this.body, serializedSenderKey));
  }
}
