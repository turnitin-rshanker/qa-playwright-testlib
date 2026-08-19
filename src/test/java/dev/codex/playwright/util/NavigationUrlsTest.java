package dev.codex.playwright.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public final class NavigationUrlsTest {

  @Test
  public void resolvesTheBaseUrlAndRelativePathExactlyOnce() {
    assertThat(NavigationUrls.resolvePageUrl("https://app.test/", "/account"))
        .isEqualTo("https://app.test/account");
  }

  @Test
  public void masksQueryAndFragmentValuesInNavigationLogs() {
    assertThat(NavigationUrls.safeForLog(
        "https://app.test/account?access_token=secret#session=private"))
        .isEqualTo("https://app.test/account?<redacted>#<redacted>");
    assertThat(NavigationUrls.safeForLog("/account#token=secret"))
        .isEqualTo("/account#<redacted>");
    assertThat(NavigationUrls.safeForLog(
        "https://user:password@app.test/account?token=secret"))
        .isEqualTo("https://<redacted>@app.test/account?<redacted>");
  }
}
