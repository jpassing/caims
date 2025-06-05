package com.google.solutions.caims.workload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AttestationTokenTest {

  //---------------------------------------------------------------------------
  // encrypt.
  //---------------------------------------------------------------------------

  @Test
  public void verify_whenMalformed() {
    var token = new AttestationToken("eyJhbGciOiJSUzI1NiIsImtpZCI6ImFjM.AAAA.AAAA");
    assertThrows(
      IllegalArgumentException.class,
      () -> token.verify("audience", true));
  }
}
