package dev.codex.playwright.core;

import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Page;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class BrowserConsoleCapture {
  private static final int MAX_EVENTS = 1_000;

  private final BoundedEventBuffer events = new BoundedEventBuffer(MAX_EVENTS);

  void attach(Page page) {
    page.onConsoleMessage(this::recordConsoleMessage);
    page.onPageError(error -> record("PAGE_ERROR", page.url(), "", error));
  }

  Path write(Path directory) throws IOException {
    Files.createDirectories(directory);
    BoundedEventBuffer.Snapshot snapshot = events.snapshot();
    List<String> output = new ArrayList<>();
    output.add("Browser console generated at " + Instant.now());
    output.add("timestamp | level | page | location | message");
    if (snapshot.droppedEvents() > 0) {
      output.add("# Dropped oldest events after reaching the "
          + snapshot.capacity() + " event limit: " + snapshot.droppedEvents());
    }
    if (snapshot.events().isEmpty()) {
      output.add("# No browser console messages or unhandled page errors were captured.");
    } else {
      output.addAll(snapshot.events());
    }

    Path file = directory.resolve("browser-console.log");
    Files.write(file, output, StandardCharsets.UTF_8);
    return file;
  }

  private void recordConsoleMessage(ConsoleMessage message) {
    try {
      Page sourcePage = message.page();
      String pageUrl = sourcePage == null ? "" : sourcePage.url();
      record(message.type().toUpperCase(), pageUrl, message.location(), message.text());
    } catch (RuntimeException exception) {
      record("CAPTURE_ERROR", "", "", exception.getMessage());
    }
  }

  private void record(String level, String pageUrl, String location, String message) {
    events.add(Instant.now()
        + " | " + sanitizeField(level)
        + " | " + sanitizeField(pageUrl)
        + " | " + sanitizeField(location)
        + " | " + sanitizeField(message));
  }

  private static String sanitizeField(String value) {
    String sanitized = SecretRedactor.redact(value == null ? "" : value);
    return sanitized.replace('\r', ' ').replace('\n', ' ');
  }
}

