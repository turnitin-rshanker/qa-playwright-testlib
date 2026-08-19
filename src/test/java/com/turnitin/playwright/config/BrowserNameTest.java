package com.turnitin.playwright.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

public final class BrowserNameTest {

  @Test
  public void parsesEnginesAndBrandedChromiumChannels() {
    assertThat(BrowserName.parse("chromium")).isEqualTo(BrowserName.CHROMIUM);
    assertThat(BrowserName.parse("google-chrome")).isEqualTo(BrowserName.CHROME);
    assertThat(BrowserName.parse("edge")).isEqualTo(BrowserName.MSEDGE);
    assertThat(BrowserName.parse("firefox")).isEqualTo(BrowserName.FIREFOX);
    assertThat(BrowserName.parse("webkit")).isEqualTo(BrowserName.WEBKIT);
    assertThat(BrowserName.MSEDGE.channel()).isEqualTo("msedge");
    assertThat(BrowserName.FIREFOX.channel()).isNull();
  }

  @Test
  public void rejectsUnsupportedBrowsersWithAvailableValues() {
    assertThatThrownBy(() -> BrowserName.parse("opera"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("chromium, chrome, msedge, firefox, or webkit");
  }
}

