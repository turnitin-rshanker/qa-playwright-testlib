package com.turnitin.playwright.config;

import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Strict, safe loader for the environment-aware framework YAML document. */
final class YamlConfigLoader {
  private static final Set<String> ROOT_KEYS = Set.of(
      "defaultEnvironment", "defaults", "environments");
  private static final Set<String> CONFIG_KEYS = Set.of(
      "browser",
      "headless",
      "baseUrl",
      "actionTimeoutMs",
      "navigationTimeoutMs",
      "device",
      "userAgent",
      "deviceScaleFactor",
      "viewport",
      "slowMoMs",
      "ignoreHttpsErrors",
      "traceMode",
      "screenshotMode",
      "diagnosticsEnabled",
      "artifactsDirectory");
  private static final Set<String> VIEWPORT_KEYS = Set.of("width", "height");
  private static final Pattern ENVIRONMENT_NAME = Pattern.compile("[a-z0-9][a-z0-9._-]*");

  private static final Setting BROWSER = setting("browser", "browser", "PW_BROWSER", "chromium");
  private static final Setting HEADLESS = setting("headless", "headless", "PW_HEADLESS", true);
  private static final Setting BASE_URL = setting("baseUrl", "base.url", "PW_BASE_URL", "about:blank");
  private static final Setting ACTION_TIMEOUT = setting(
      "actionTimeoutMs", "action.timeout.ms", "PW_ACTION_TIMEOUT_MS", 15_000);
  private static final Setting NAVIGATION_TIMEOUT = setting(
      "navigationTimeoutMs", "navigation.timeout.ms", "PW_NAVIGATION_TIMEOUT_MS", 30_000);
  private static final Setting DEVICE = setting(
          "device", "device", "PW_DEVICE", "desktop");
  private static final Setting USER_AGENT = setting(
          "userAgent", "user.agent", "PW_USER_AGENT", "Mozilla/5.0 custom-agent");
  private static final Setting DEVICE_SCALE_FACTOR = setting(
          "deviceScaleFactor", "device.scale.factor", "PW_DEVICE_SCALE_FACTOR", 1);
  private static final Setting VIEWPORT_WIDTH = setting(
      "viewport.width", "viewport.width", "PW_VIEWPORT_WIDTH", 1_440);
  private static final Setting VIEWPORT_HEIGHT = setting(
      "viewport.height", "viewport.height", "PW_VIEWPORT_HEIGHT", 900);
  private static final Setting SLOW_MO = setting("slowMoMs", "slowmo.ms", "PW_SLOWMO_MS", 0);
  private static final Setting IGNORE_HTTPS = setting(
      "ignoreHttpsErrors", "ignore.https.errors", "PW_IGNORE_HTTPS_ERRORS", false);
  private static final Setting TRACE_MODE = setting(
      "traceMode", "trace.mode", "PW_TRACE_MODE", "retain-on-failure");
  private static final Setting SCREENSHOT_MODE = setting(
      "screenshotMode", "screenshot.mode", "PW_SCREENSHOT_MODE", "only-on-failure");
  private static final Setting DIAGNOSTICS = setting(
      "diagnosticsEnabled", "diagnostics.enabled", "PW_DIAGNOSTICS_ENABLED", true);
  private static final Setting ARTIFACTS_DIRECTORY = setting(
      "artifactsDirectory", "artifacts.dir", "PW_ARTIFACTS_DIR", "target/artifacts");

  private YamlConfigLoader() {
  }

  static FrameworkConfig load(
      InputStream input,
      Map<String, String> environmentVariables,
      Properties systemProperties) {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(environmentVariables, "environmentVariables");
    Objects.requireNonNull(systemProperties, "systemProperties");

    Map<String, Object> root = loadDocument(input);
    rejectUnknownKeys(root, ROOT_KEYS, "root");

    String defaultEnvironment = requireEnvironmentName(
        requireString(root.get("defaultEnvironment"), "defaultEnvironment"),
        "defaultEnvironment");
    Map<String, Object> defaults = flattenConfig(
        requireMapping(root.get("defaults"), "defaults"),
        "defaults");

    Map<String, Object> rawEnvironments = requireMapping(
        root.get("environments"), "environments");
    if (rawEnvironments.isEmpty()) {
      throw configError("environments must contain at least one environment");
    }

    Map<String, Map<String, Object>> environments = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rawEnvironments.entrySet()) {
      String name = requireEnvironmentName(entry.getKey(), "environment name");
      environments.put(name, flattenConfig(
          requireMapping(entry.getValue(), "environments." + name),
          "environments." + name));
    }

    String selectedEnvironment = selectEnvironment(
        defaultEnvironment, environmentVariables, systemProperties);
    Map<String, Object> selectedValues = environments.get(selectedEnvironment);
    if (selectedValues == null) {
      throw configError(
          "Unknown environment '" + selectedEnvironment + "'. Available environments: "
              + String.join(", ", new TreeSet<>(environments.keySet())));
    }

    Map<String, Object> merged = new LinkedHashMap<>(defaults);
    merged.putAll(selectedValues);
    Values values = new Values(merged, environmentVariables, systemProperties);

    return new FrameworkConfig(
        selectedEnvironment,
        BrowserName.parse(values.string(BROWSER)),
        values.bool(HEADLESS),
        values.string(BASE_URL),
        values.number(ACTION_TIMEOUT),
        values.number(NAVIGATION_TIMEOUT),
        values.string(DEVICE),
        values.string(USER_AGENT),
        values.integer(DEVICE_SCALE_FACTOR),
        values.integer(VIEWPORT_WIDTH),
        values.integer(VIEWPORT_HEIGHT),
        values.number(SLOW_MO),
        values.bool(IGNORE_HTTPS),
        TraceMode.parse(values.string(TRACE_MODE)),
        ScreenshotMode.parse(values.string(SCREENSHOT_MODE)),
        values.bool(DIAGNOSTICS),
        values.path(ARTIFACTS_DIRECTORY));
  }

  private static Map<String, Object> loadDocument(InputStream input) {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    options.setMaxAliasesForCollections(0);
    options.setNestingDepthLimit(20);
    options.setCodePointLimit(1_000_000);

    try {
      Object document = new Yaml(new SafeConstructor(options)).load(input);
      return requireMapping(document, "root");
    } catch (YAMLException exception) {
      throw configError(
          "Could not parse " + FrameworkConfig.RESOURCE_NAME
              + "; verify its YAML syntax and remove duplicate keys");
    }
  }

  private static Map<String, Object> flattenConfig(
      Map<String, Object> source,
      String path) {
    rejectUnknownKeys(source, CONFIG_KEYS, path);
    Map<String, Object> flattened = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      if (entry.getValue() == null) {
        throw configError(path + "." + entry.getKey() + " must not be null");
      }
      if (!entry.getKey().equals("viewport")) {
        flattened.put(entry.getKey(), entry.getValue());
        continue;
      }

      Map<String, Object> viewport = requireMapping(entry.getValue(), path + ".viewport");
      rejectUnknownKeys(viewport, VIEWPORT_KEYS, path + ".viewport");
      for (Map.Entry<String, Object> viewportEntry : viewport.entrySet()) {
        if (viewportEntry.getValue() == null) {
          throw configError(path + ".viewport." + viewportEntry.getKey() + " must not be null");
        }
        flattened.put("viewport." + viewportEntry.getKey(), viewportEntry.getValue());
      }
    }
    return flattened;
  }

  private static String selectEnvironment(
      String fallback,
      Map<String, String> environmentVariables,
      Properties systemProperties) {
    String systemValue = trimmed(systemProperties.getProperty("env"));
    if (systemValue != null) {
      return requireEnvironmentName(systemValue, "env system property");
    }
    String environmentValue = trimmed(environmentVariables.get("PW_ENV"));
    if (environmentValue != null) {
      return requireEnvironmentName(environmentValue, "PW_ENV");
    }
    return fallback;
  }

  private static void rejectUnknownKeys(
      Map<String, Object> values,
      Set<String> allowed,
      String path) {
    for (String key : values.keySet()) {
      if (!allowed.contains(key)) {
        throw configError("Unknown configuration key '" + path + "." + key + "'");
      }
    }
  }

  private static Map<String, Object> requireMapping(Object value, String path) {
    if (!(value instanceof Map<?, ?> raw)) {
      throw configError(path + " must be a YAML mapping");
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : raw.entrySet()) {
      if (!(entry.getKey() instanceof String key) || key.isBlank()) {
        throw configError(path + " contains a non-text or blank key");
      }
      result.put(key, entry.getValue());
    }
    return result;
  }

  private static String requireString(Object value, String path) {
    if (!(value instanceof String text) || text.isBlank()) {
      throw configError(path + " must be non-blank text");
    }
    return text.trim();
  }

  private static String requireEnvironmentName(String value, String path) {
    String environment = value.trim();
    if (!ENVIRONMENT_NAME.matcher(environment).matches()) {
      throw configError(
          path + " must use lower-case letters, numbers, dots, underscores, or hyphens");
    }
    return environment;
  }

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Setting setting(
      String yamlKey,
      String systemProperty,
      String environmentVariable,
      Object fallback) {
    return new Setting(yamlKey, systemProperty, environmentVariable, fallback);
  }

  private static IllegalArgumentException configError(String message) {
    return new IllegalArgumentException(
        "Invalid " + FrameworkConfig.RESOURCE_NAME + ": " + message);
  }

  private record Setting(
      String yamlKey,
      String systemProperty,
      String environmentVariable,
      Object fallback) {
  }

  private record Values(
      Map<String, Object> yaml,
      Map<String, String> environmentVariables,
      Properties systemProperties) {

    private Object raw(Setting setting) {
      String systemValue = trimmed(systemProperties.getProperty(setting.systemProperty()));
      if (systemValue != null) {
        return systemValue;
      }
      String environmentValue = trimmed(environmentVariables.get(setting.environmentVariable()));
      if (environmentValue != null) {
        return environmentValue;
      }
      return yaml.getOrDefault(setting.yamlKey(), setting.fallback());
    }

    private String string(Setting setting) {
      Object value = raw(setting);
      if (!(value instanceof String text) || text.isBlank()) {
        throw configError(setting.yamlKey() + " must be non-blank text");
      }
      return text.trim();
    }

    private boolean bool(Setting setting) {
      Object value = raw(setting);
      if (value instanceof Boolean booleanValue) {
        return booleanValue;
      }
      if (value instanceof String text
          && (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))) {
        return Boolean.parseBoolean(text);
      }
      throw configError(setting.yamlKey() + " must be true or false");
    }

    private int integer(Setting setting) {
      Object value = raw(setting);
      if (value instanceof Number number) {
        double decimal = number.doubleValue();
        if (Double.isFinite(decimal)
            && decimal == Math.rint(decimal)
            && decimal >= Integer.MIN_VALUE
            && decimal <= Integer.MAX_VALUE) {
          return (int) decimal;
        }
        throw configError(setting.yamlKey() + " must be an integer");
      }
      if (value instanceof String text) {
        try {
          return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
          // Fall through to the sanitized error below.
        }
      }
      throw configError(setting.yamlKey() + " must be an integer");
    }

    private double number(Setting setting) {
      Object value = raw(setting);
      double parsed;
      if (value instanceof Number number) {
        parsed = number.doubleValue();
      } else if (value instanceof String text) {
        try {
          parsed = Double.parseDouble(text);
        } catch (NumberFormatException exception) {
          throw configError(setting.yamlKey() + " must be a number");
        }
      } else {
        throw configError(setting.yamlKey() + " must be a number");
      }
      if (!Double.isFinite(parsed)) {
        throw configError(setting.yamlKey() + " must be a finite number");
      }
      return parsed;
    }

    private Path path(Setting setting) {
      try {
        return Path.of(string(setting));
      } catch (InvalidPathException exception) {
        throw configError(setting.yamlKey() + " is not a valid filesystem path");
      }
    }
  }
}
