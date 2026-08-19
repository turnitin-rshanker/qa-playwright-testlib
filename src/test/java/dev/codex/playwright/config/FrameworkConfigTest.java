package dev.codex.playwright.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.Test;

public final class FrameworkConfigTest {

  @Test
  public void mergesDefaultsEnvironmentVariablesAndSystemPropertiesInOrder() {
    Properties systemProperties = new Properties();
    systemProperties.setProperty("env", "qa");
    systemProperties.setProperty("browser", "webkit");
    systemProperties.setProperty("headless", "false");

    FrameworkConfig config = load("""
        defaultEnvironment: local
        defaults:
          browser: chromium
          headless: true
          baseUrl: about:blank
          actionTimeoutMs: 15000
          viewport:
            width: 1440
            height: 900
        environments:
          local: {}
          qa:
            baseUrl: https://qa.example.test
            viewport:
              width: 1280
          staging:
            baseUrl: https://staging.example.test
        """, Map.of(
            "PW_ENV", "staging",
            "PW_BROWSER", "firefox",
            "PW_ACTION_TIMEOUT_MS", "2500"), systemProperties);

    assertThat(config.environment()).isEqualTo("qa");
    assertThat(config.browser()).isEqualTo(BrowserName.WEBKIT);
    assertThat(config.headless()).isFalse();
    assertThat(config.baseUrl()).isEqualTo("https://qa.example.test");
    assertThat(config.actionTimeoutMs()).isEqualTo(2500);
    assertThat(config.viewportWidth()).isEqualTo(1280);
    assertThat(config.viewportHeight()).isEqualTo(900);
  }

  @Test
  public void usesEnvironmentVariableSelectionWhenSystemSelectionIsAbsent() {
    FrameworkConfig config = load("""
        defaultEnvironment: local
        defaults:
          browser: chromium
          artifactsDirectory: build/evidence
        environments:
          local: {}
          staging:
            browser: firefox
            diagnosticsEnabled: false
        """, Map.of("PW_ENV", "staging"), new Properties());

    assertThat(config.environment()).isEqualTo("staging");
    assertThat(config.browser()).isEqualTo(BrowserName.FIREFOX);
    assertThat(config.diagnosticsEnabled()).isFalse();
    assertThat(config.artifactsDirectory()).isEqualTo(Path.of("build/evidence"));
  }

  @Test
  public void rejectsUnknownEnvironmentsWithAvailableNames() {
    Properties systemProperties = new Properties();
    systemProperties.setProperty("env", "production");

    assertThatThrownBy(() -> load(minimalYaml(), Map.of(), systemProperties))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown environment 'production'")
        .hasMessageContaining("local, qa");
  }

  @Test
  public void rejectsUnknownConfigurationKeys() {
    assertThatThrownBy(() -> load("""
        defaultEnvironment: local
        defaults:
          browser: chromium
          browserName: firefox
        environments:
          local: {}
        """, Map.of(), new Properties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown configuration key 'defaults.browserName'");
  }

  @Test
  public void rejectsDuplicateYamlKeysWithoutEchoingTheirValues() {
    assertThatThrownBy(() -> load("""
        defaultEnvironment: local
        defaults:
          baseUrl: https://first.example.test/private-value
          baseUrl: https://second.example.test/other-value
        environments:
          local: {}
        """, Map.of(), new Properties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Could not parse env-local.yaml")
        .satisfies(exception -> assertThat(exception.getMessage())
            .doesNotContain("private-value", "other-value"));
  }

  @Test
  public void rejectsWrongValueTypesWithSanitizedErrors() {
    assertThatThrownBy(() -> load("""
        defaultEnvironment: local
        defaults:
          headless: sometimes
        environments:
          local: {}
        """, Map.of(), new Properties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid env-local.yaml: headless must be true or false")
            .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("sometimes"));
  }

  @Test
  public void rejectsMalformedBaseUrlsWithoutEchoingTheirValues() {
    assertThatThrownBy(() -> load("""
        defaultEnvironment: local
        defaults:
          baseUrl: https://bad host.test/private-value
        environments:
          local: {}
        """, Map.of(), new Properties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("baseUrl is malformed")
        .satisfies(exception -> assertThat(exception.getMessage())
            .doesNotContain("bad host", "private-value"));
  }

  private static String minimalYaml() {
    return """
        defaultEnvironment: local
        defaults:
          browser: chromium
        environments:
          local: {}
          qa: {}
        """;
  }

  private static FrameworkConfig load(
      String yaml,
      Map<String, String> environmentVariables,
      Properties systemProperties) {
    return YamlConfigLoader.load(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
        environmentVariables,
        systemProperties);
  }
}
