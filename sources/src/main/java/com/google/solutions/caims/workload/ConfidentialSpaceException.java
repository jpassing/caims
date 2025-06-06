package com.google.solutions.caims.workload;

import java.io.IOException;

public class ConfidentialSpaceException extends IOException {
  public ConfidentialSpaceException(String message) {
    super(message);
  }
}
