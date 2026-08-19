package dev.codex.playwright.config;

import dev.codex.playwright.util.NavigationUrls;
import dev.codex.playwright.util.RuntimeConfig;

import java.io.InputStream;
import java.nio.file.Path;

public record FrameworkConfig(
    String environment,
    BrowserName browser,
    boolean headless,
    String baseUrl,
    double actionTimeoutMs,
    double navigationTimeoutMs,
    int viewportWidth,
    int viewportHeight,
    double slowMoMs,
    boolean ignoreHttpsErrors,
    TraceMode traceMode,
    ScreenshotMode screenshotMode,
    boolean diagnosticsEnabled,
    Path artifactsDirectory) {

  static final String RESOURCE_NAME = String.format("env-%s.yaml", RuntimeConfig.getTestEnv());

  public FrameworkConfig {
    environment = requireText(environment, "environment");
    baseUrl = NavigationUrls.requireBaseUrl(baseUrl);
    requirePositive("actionTimeoutMs", actionTimeoutMs);
    requirePositive("navigationTimeoutMs", navigationTimeoutMs);
    requirePositive("viewport.width", viewportWidth);
    requirePositive("viewport.height", viewportHeight);
    if (slowMoMs < 0) {
      throw new IllegalArgumentException("slowMoMs must be zero or greater");
    }
  }

  public static FrameworkConfig load() {
    ClassLoader classLoader = FrameworkConfig.class.getClassLoader();
    try (InputStream input = classLoader.getResourceAsStream(RESOURCE_NAME)) {
      if (input == null) {
        throw new IllegalStateException(
            "Could not find " + RESOURCE_NAME + " on the test classpath");
      }
      return YamlConfigLoader.load(input, System.getenv(), System.getProperties());
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Could not close " + RESOURCE_NAME, exception);
    }
  }

  public FrameworkConfig withBrowser(BrowserName browserOverride) {
    return new FrameworkConfig(
        environment,
        browserOverride,
        headless,
        baseUrl,
        actionTimeoutMs,
        navigationTimeoutMs,
        viewportWidth,
        viewportHeight,
        slowMoMs,
        ignoreHttpsErrors,
        traceMode,
        screenshotMode,
        diagnosticsEnabled,
        artifactsDirectory);
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }

  private static void requirePositive(String key, double value) {
    if (value <= 0) {
      throw new IllegalArgumentException(key + " must be greater than zero");
    }
  }
}
