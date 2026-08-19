package dev.codex.playwright.core;

import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Timing;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class NetworkCapture {
  private static final int MAX_EVENTS = 2_000;

  private final BoundedEventBuffer events = new BoundedEventBuffer(MAX_EVENTS);

  void recordRequest(Request request) {
    try {
      record("request", request, null, null);
    } catch (RuntimeException exception) {
      recordCaptureError("request", exception);
    }
  }

  void recordResponse(Response response) {
    try {
      record("response", response.request(), response, null);
    } catch (RuntimeException exception) {
      recordCaptureError("response", exception);
    }
  }

  void recordRequestFailed(Request request) {
    try {
      record("request_failed", request, null, request.failure());
    } catch (RuntimeException exception) {
      recordCaptureError("request_failed", exception);
    }
  }

  Path write(Path directory) throws IOException {
    Files.createDirectories(directory);
    BoundedEventBuffer.Snapshot snapshot = events.snapshot();
    List<String> output = new ArrayList<>();
    output.add(jsonObject(
        field("event", "capture_summary"),
        field("generatedAt", Instant.now().toString()),
        numberField("eventLimit", snapshot.capacity()),
        numberField("droppedEvents", snapshot.droppedEvents())));
    output.addAll(snapshot.events());

    Path file = directory.resolve("network-calls.jsonl");
    Files.write(file, output, StandardCharsets.UTF_8);
    return file;
  }

  private void record(String event, Request request, Response response, String failure) {
    String status = response == null ? null : Integer.toString(response.status());
    String ok = response == null ? null : Boolean.toString(response.ok());
    String fromServiceWorker = response == null ? null : Boolean.toString(response.fromServiceWorker());
    String duration = response == null ? null : durationMs(request);

    events.add(jsonObject(
        field("timestamp", Instant.now().toString()),
        field("event", event),
        field("method", request.method()),
        field("resourceType", request.resourceType()),
        field("url", SecretRedactor.redact(request.url())),
        booleanField("navigationRequest", request.isNavigationRequest()),
        rawField("status", status),
        field("statusText", response == null ? null : response.statusText()),
        rawField("ok", ok),
        rawField("fromServiceWorker", fromServiceWorker),
        rawField("durationMs", duration),
        field("failure", SecretRedactor.redact(failure))));
  }

  private static String durationMs(Request request) {
    try {
      Timing timing = request.timing();
      if (timing == null || timing.responseEnd < 0) {
        return null;
      }
      return String.format(Locale.ROOT, "%.3f", timing.responseEnd);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private void recordCaptureError(String sourceEvent, RuntimeException exception) {
    events.add(jsonObject(
        field("timestamp", Instant.now().toString()),
        field("event", "capture_error"),
        field("sourceEvent", sourceEvent),
        field("message", SecretRedactor.redact(exception.getMessage()))));
  }

  private static String jsonObject(String... fields) {
    return "{" + String.join(",", fields) + "}";
  }

  private static String field(String name, String value) {
    return quote(name) + ":" + (value == null ? "null" : quote(value));
  }

  private static String numberField(String name, int value) {
    return quote(name) + ":" + value;
  }

  private static String booleanField(String name, boolean value) {
    return quote(name) + ":" + value;
  }

  private static String rawField(String name, String value) {
    return quote(name) + ":" + (value == null ? "null" : value);
  }

  private static String quote(String value) {
    return "\"" + escapeJson(value) + "\"";
  }

  private static String escapeJson(String value) {
    if (value == null) {
      return "";
    }

    StringBuilder escaped = new StringBuilder(value.length() + 16);
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '\"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (character < 0x20) {
            escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
          } else {
            escaped.append(character);
          }
        }
      }
    }
    return escaped.toString();
  }
}

