package com.turnitin.playwright.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public final class BoundedEventBufferTest {

  @Test
  public void retainsTheNewestEventsWithinItsLimit() {
    BoundedEventBuffer buffer = new BoundedEventBuffer(2);

    buffer.add("first");
    buffer.add("second");
    buffer.add("third");

    BoundedEventBuffer.Snapshot snapshot = buffer.snapshot();
    assertThat(snapshot.events()).containsExactly("second", "third");
    assertThat(snapshot.droppedEvents()).isEqualTo(1);
    assertThat(snapshot.capacity()).isEqualTo(2);
  }
}
