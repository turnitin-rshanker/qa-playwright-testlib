package dev.codex.playwright.core;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class BrowserDiagnostics {
  private static final int MAX_EVENTS = 500;

  private final Deque<String> events = new ArrayDeque<>();
  private final BrowserConsoleCapture consoleCapture = new BrowserConsoleCapture();
  private final NetworkCapture networkCapture = new NetworkCapture();
  private int droppedEvents;

  void attach(BrowserContext context) {
    context.onPage(this::attach);
    context.onRequest(networkCapture::recordRequest);
    context.onResponse(response -> {
      networkCapture.recordResponse(response);
      if (response.status() >= 400) {
        record("HTTP_" + response.status() + " | " + response.request().method() + " " + response.url());
      }
    });
    context.onRequestFailed(request -> {
      networkCapture.recordRequestFailed(request);
      record("REQUEST_FAILED | " + request.method() + " " + request.url() + " | " + request.failure());
    });
  }

  void attach(Page page) {
    consoleCapture.attach(page);
    page.onConsoleMessage(message ->
        record("CONSOLE " + message.type().toUpperCase() + " | " + message.text()));
    page.onPageError(error -> record("PAGE_ERROR | " + error));
  }

  void recordFailure(Throwable failure) {
    if (failure != null) {
      record("TEST_FAILURE | " + failure.getClass().getName() + ": " + failure.getMessage());
    }
  }

  List<Path> write(Path directory) {
    List<Path> files = new ArrayList<>();
    try {
      files.add(consoleCapture.write(directory));
    } catch (IOException | RuntimeException exception) {
      reportWriteProblem("browser console", exception);
    }
    try {
      files.add(networkCapture.write(directory));
    } catch (IOException | RuntimeException exception) {
      reportWriteProblem("network calls", exception);
    }

    BoundedEventBuffer.Snapshot snapshot;
    synchronized (this) {
      snapshot = new BoundedEventBuffer.Snapshot(List.copyOf(events), droppedEvents, MAX_EVENTS);
    }
    List<String> output = new ArrayList<>();
    output.add("Browser diagnostics generated at " + Instant.now());
    if (snapshot.droppedEvents() > 0) {
      output.add("Dropped oldest events after reaching the "
          + snapshot.capacity() + " event limit: " + snapshot.droppedEvents());
    }
    if (snapshot.events().isEmpty()) {
      output.add("No console errors, page errors, failed requests, or HTTP 4xx/5xx responses were captured.");
    } else {
      output.addAll(snapshot.events());
    }

    try {
      Files.createDirectories(directory);
      Path file = directory.resolve("browser-diagnostics.log");
      Files.write(file, output, StandardCharsets.UTF_8);
      files.add(file);
    } catch (IOException | RuntimeException exception) {
      reportWriteProblem("browser diagnostics", exception);
    }
    return files;
  }

  private synchronized void record(String message) {
    if (events.size() == MAX_EVENTS) {
      events.removeFirst();
      droppedEvents++;
    }
    events.addLast(Instant.now() + " | " + SecretRedactor.redact(message));
  }

  private static void reportWriteProblem(String reportName, Exception exception) {
    System.err.println("Could not write " + reportName + " report: " + exception.getMessage());
  }
}
