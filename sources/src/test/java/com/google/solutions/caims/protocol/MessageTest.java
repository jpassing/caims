package com.google.solutions.caims.protocol;

import com.google.crypto.tink.hybrid.HybridConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

  @BeforeAll
  public static void setup() throws Exception {
    HybridConfig.register();
  }

  //---------------------------------------------------------------------------
  // toString.
  //---------------------------------------------------------------------------

  @Test
  public void toString_returnsClearText() {
    var message = new Message("Test", null);
    assertEquals("Test", message.toString());
  }

  //---------------------------------------------------------------------------
  // encrypt.
  //---------------------------------------------------------------------------

  @Test
  public void encrypt_withoutSenderPublicKey() throws Exception {
    var recipientKeyPair = RequestEncryptionKeyPair.generate();

    var message = new Message("Test", null);
    var decryptedMessage = message
      .encrypt(recipientKeyPair.publicKey())
      .decrypt(recipientKeyPair.privateKey());

    assertEquals("Test", decryptedMessage.toString());
    assertNull(decryptedMessage.senderPublicKey());
  }

  @Test
  public void encrypt_withSenderPublicKey() throws Exception {
    var recipientKeyPair = RequestEncryptionKeyPair.generate();
    var senderKeyPair = RequestEncryptionKeyPair.generate();

    var message = new Message("Test", senderKeyPair.publicKey());
    var decryptedMessage = message
      .encrypt(recipientKeyPair.publicKey())
      .decrypt(recipientKeyPair.privateKey());

    assertEquals("Test", decryptedMessage.toString());
    assertNotNull(decryptedMessage.senderPublicKey());
    assertEquals(decryptedMessage.senderPublicKey(), senderKeyPair.publicKey());
  }
}
