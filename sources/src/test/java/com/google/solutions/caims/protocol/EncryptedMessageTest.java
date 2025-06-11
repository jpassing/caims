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

}
