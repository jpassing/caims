package com.google.solutions.caims.broker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBroker {
  @Test
  public void idenfifier_toString() {
    var id = new Broker.Endpoint("123", "us-central1");
    assertEquals("https://broker-123.us-central1.run.app/", id.toString());
  }
}
