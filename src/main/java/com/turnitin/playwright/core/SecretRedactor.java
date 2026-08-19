package com.turnitin.playwright.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SecretRedactor {
  private static final Pattern SECRET = Pattern.compile(
      "(?i)(\\b(?:access[_-]?token|refresh[_-]?token|token|api[_-]?key|apikey|password|passwd|secret|authorization|session|code)\\s*[:=]\\s*)([^\\s&#,;]+)");

  private SecretRedactor() {
  }

  static String redact(String text) {
    if (text == null || text.isBlank()) {
      return text;
    }
    Matcher matcher = SECRET.matcher(text);
    StringBuilder sanitized = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(
          sanitized,
          Matcher.quoteReplacement(matcher.group(1) + "<redacted>"));
    }
    matcher.appendTail(sanitized);
    return sanitized.toString();
  }
}

