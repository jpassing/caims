package com.google.solutions.caims.protocol;

import com.google.crypto.tink.hybrid.HybridConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptedMessageTest {

  @BeforeAll
  public static void setup() throws Exception {
    HybridConfig.register();
  }

  //---------------------------------------------------------------------------
  // write.
  //---------------------------------------------------------------------------

  @Test
  public void write() throws Exception {
    var pair = RequestEncryptionKeyPair.generate();

    try (var buffer = new ByteArrayOutputStream();
         var stream = new DataOutputStream(buffer)) {
      new Message("Test", null)
        .encrypt(pair.publicKey())
        .write(stream);

      assertNotEquals(0, buffer.size());
    }
  }

  //---------------------------------------------------------------------------
  // read.
  //---------------------------------------------------------------------------

  @Test
  public void read() throws Exception {
    var pair = RequestEncryptionKeyPair.generate();

    try (var buffer = new ByteArrayOutputStream()) {
      try (var stream = new DataOutputStream(buffer)) {
        new Message("Test", null)
          .encrypt(pair.publicKey())
          .write(stream);
      }

      try (var stream = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
        var message = EncryptedMessage.read(stream, 1024);
        assertEquals("Test", message.decrypt(pair.privateKey()).toString());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3, 1025})
  public void read_whenDataInvalid(int size) throws Exception {
    try (var buffer = new ByteArrayOutputStream();
         var stream = new DataInputStream(new ByteArrayInputStream(new byte[size]))) {
      assertThrows(
        IOException.class,
        () -> EncryptedMessage.read(stream, 1024));
    }
  }
}
