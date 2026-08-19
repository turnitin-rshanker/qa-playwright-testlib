package dev.codex.playwright.testng;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public final class RetryTestAnalyzer implements IRetryAnalyzer {

  private final int maxRetries = configuredRetries();

  private int retryCount;

  @Override
  public boolean retry(ITestResult result) {
    if (!result.isSuccess() && retryCount < maxRetries) {
      retryCount++;
      return true;
    }
    return false;
  }

  public String getResultStatusName(int status) {
    return switch (status) {
      case ITestResult.SUCCESS -> "SUCCESS";
      case ITestResult.FAILURE -> "FAILURE";
      case ITestResult.SKIP -> "SKIP";
      default -> "UNKNOWN";
    };
  }

  static int configuredRetries() {
    String configured = System.getProperty(
        "retries",
        System.getProperty("RETRIES", "0"));
    return parseRetries(configured);
  }

  static int parseRetries(String configured) {
    try {
      int retries = Integer.parseInt(configured);
      if (retries < 0) {
        throw new IllegalArgumentException("retries cannot be negative: " + configured);
      }
      return retries;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("retries must be an integer: " + configured, exception);
    }
  }

}
