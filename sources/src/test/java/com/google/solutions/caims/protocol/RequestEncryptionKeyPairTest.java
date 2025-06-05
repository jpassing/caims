package com.google.solutions.caims.protocol;

import com.google.crypto.tink.hybrid.EciesParameters;
import com.google.crypto.tink.hybrid.HybridConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

public class RequestEncryptionKeyPairTest {

  @BeforeAll
  public static void setup() throws Exception {
    HybridConfig.register();
  }

  //---------------------------------------------------------------------------
  // generate.
  //---------------------------------------------------------------------------

  @Test
  public void generate_createsPublicAndPrivateKey() throws Exception {
    var pair = RequestEncryptionKeyPair.generate();
    assertNotNull(pair.privateKey());
    assertNotNull(pair.publicKey());
  }

  @Test
  public void generate_usesP256() throws Exception {
    var parameters = RequestEncryptionKeyPair
      .generate()
      .publicKey()
      .parameters();
    var ecies = assertInstanceOf(EciesParameters.class, parameters);

    assertEquals(EciesParameters.CurveType.NIST_P256, ecies.getCurveType());
  }

  //---------------------------------------------------------------------------
  // encrypt.
  //---------------------------------------------------------------------------

  @Test
  public void encrypt() throws Exception {
    var pair = RequestEncryptionKeyPair.generate();

    var clearText = "test".getBytes(StandardCharsets.US_ASCII);
    var cipherText = pair
      .publicKey()
      .encrypt(clearText, null);

    var decrypted = pair
      .privateKey()
      .decrypt(cipherText, null);

    assertArrayEquals(clearText, decrypted);
  }

  @Test
  public void encrypt_whenAssociatedDataDoesNotMatch() throws Exception {
    var pair = RequestEncryptionKeyPair.generate();

    var clearText = "test".getBytes(StandardCharsets.US_ASCII);
    var cipherText = pair
      .publicKey()
      .encrypt(clearText, "associated-data".getBytes(StandardCharsets.US_ASCII));

    assertThrows(
      GeneralSecurityException.class,
      () -> pair
        .privateKey()
        .decrypt(cipherText, "wrong-data".getBytes(StandardCharsets.US_ASCII)));
  }
}
