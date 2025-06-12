package com.google.solutions.caims.workload;

import java.io.IOException;

public class ConfidentialSpaceException extends IOException {
  ConfidentialSpaceException(String message) {
    super(message);
  }
}
