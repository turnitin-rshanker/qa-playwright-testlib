package com.turnitin.playwright.util;


import lombok.Getter;

import java.time.Duration;

@Getter
public enum TimeSpan {
  NONE(0L),
  POLLING(250L),
  WAITING_FOR_ELEMENT(30000L),
  ALERT(10000L),
  FRAME(10000L),
  SHORT(1000L),
  PAUSE(100L),
  PAGE_SYNC(60000L),
  DOWNLOAD(120000L),
  SCRIPT(30000L),
  SMALL(10000L),
  MEDIUM(60000L),
  LARGE(120000L),
  EXTREME(300000L);

  private final long millis;

  TimeSpan(long millis) {
    this.millis = millis;
  }

  public Duration asDuration() {
    return Duration.ofMillis(millis);
  }

}
