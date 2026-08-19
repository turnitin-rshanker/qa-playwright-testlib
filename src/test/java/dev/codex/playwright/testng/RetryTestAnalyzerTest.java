package dev.codex.playwright.testng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

public final class RetryTestAnalyzerTest {

  @Test
  public void acceptsZeroAndPositiveRetryCounts() {
    assertThat(RetryTestAnalyzer.parseRetries("0")).isZero();
    assertThat(RetryTestAnalyzer.parseRetries("2")).isEqualTo(2);
  }

  @Test
  public void rejectsNegativeRetryCounts() {
    assertThatThrownBy(() -> RetryTestAnalyzer.parseRetries("-1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be negative");
  }

  @Test
  public void rejectsNonNumericRetryCounts() {
    assertThatThrownBy(() -> RetryTestAnalyzer.parseRetries("sometimes"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be an integer");
  }
}
