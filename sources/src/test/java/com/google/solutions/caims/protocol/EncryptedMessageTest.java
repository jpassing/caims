package com.google.solutions.caims.protocol;

import com.google.crypto.tink.hybrid.HybridConfig;
import org.junit.jupiter.api.BeforeAll;

public class EncryptedMessageTest {

  @BeforeAll
  public static void setup() throws Exception {
    HybridConfig.register();
  }

}
