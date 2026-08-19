package com.turnitin.playwright.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public final class SecretRedactorTest {

  @Test
  public void redactsSecretsInQueryStringsAndFragments() {
    String input = "https://example.test/items?token=query-secret&safe=visible#access_token=fragment-secret";

    String result = SecretRedactor.redact(input);

    assertThat(result)
        .doesNotContain("query-secret", "fragment-secret")
        .contains("token=<redacted>", "safe=visible", "access_token=<redacted>");
  }

  @Test
  public void preservesNonSensitiveDiagnosticText() {
    String input = "GET https://example.test/items?page=2 returned HTTP 404";

    assertThat(SecretRedactor.redact(input)).isEqualTo(input);
  }
}
