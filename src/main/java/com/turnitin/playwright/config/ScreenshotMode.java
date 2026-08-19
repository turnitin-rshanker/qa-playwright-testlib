package com.turnitin.playwright.config;

import java.util.Locale;

public enum ScreenshotMode {
  OFF,
  ON,
  ONLY_ON_FAILURE;

  public static ScreenshotMode parse(String value) {
    try {
      return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "Unsupported screenshot.mode '" + value + "'. Use off, on, or only-on-failure.", exception);
    }
  }
}

