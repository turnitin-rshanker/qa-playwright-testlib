package com.turnitin.playwright.config;

import java.util.Locale;

public enum TraceMode {
  OFF,
  ON,
  RETAIN_ON_FAILURE;

  public static TraceMode parse(String value) {
    try {
      return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "Unsupported trace.mode '" + value + "'. Use off, on, or retain-on-failure.", exception);
    }
  }
}

