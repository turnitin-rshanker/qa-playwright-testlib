package dev.codex.playwright.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class BoundedEventBuffer {
  private final int capacity;
  private final Deque<String> events = new ArrayDeque<>();
  private int droppedEvents;

  BoundedEventBuffer(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be greater than zero");
    }
    this.capacity = capacity;
  }

  synchronized void add(String event) {
    if (events.size() == capacity) {
      events.removeFirst();
      droppedEvents++;
    }
    events.addLast(event);
  }

  synchronized Snapshot snapshot() {
    return new Snapshot(List.copyOf(events), droppedEvents, capacity);
  }

  record Snapshot(List<String> events, int droppedEvents, int capacity) {
    Snapshot {
      events = new ArrayList<>(events);
    }
  }
}

