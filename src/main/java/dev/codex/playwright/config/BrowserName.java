package dev.codex.playwright.config;

import java.util.Locale;

public enum BrowserName {
  CHROMIUM,
  CHROME,
  MSEDGE,
  FIREFOX,
  WEBKIT;

  public static BrowserName parse(String value) {
    try {
      String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
      return switch (normalized) {
        case "EDGE", "MICROSOFT_EDGE" -> MSEDGE;
        case "GOOGLE_CHROME" -> CHROME;
        default -> valueOf(normalized);
      };
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "Unsupported browser '" + value
              + "'. Use chromium, chrome, msedge, firefox, or webkit.", exception);
    }
  }

  public boolean isChromiumBased() {
    return this == CHROMIUM || this == CHROME || this == MSEDGE;
  }

  public String channel() {
    return switch (this) {
      case CHROME -> "chrome";
      case MSEDGE -> "msedge";
      default -> null;
    };
  }
}
