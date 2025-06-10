package com.google.solutions.caims.broker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBroker {
  @Test
  public void idenfifier_toString() {
    var id = new Broker.Identifier("123");
    assertEquals("urn:com:google:solutions:caims:123", id.toString());
  }
}
