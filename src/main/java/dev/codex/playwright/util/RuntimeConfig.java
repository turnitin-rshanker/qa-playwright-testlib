package dev.codex.playwright.util;

public class RuntimeConfig {

  private RuntimeConfig() {
    throw new RuntimeException("Utility class");
  }

  public static String getTestEnv() {
    String testEnv = getConfigValue("TEST_ENV", "test.env");
    String functionalEnv = getConfigValue("FUNCTIONAL_ENV", "functional.env");

    if (functionalEnv == null) {
      if (testEnv != null) {
        functionalEnv = testEnv;
      }
    }

    return (functionalEnv != null) ? functionalEnv :
            "local";
  }

  public static String getDriverType() {
    String driverType = getConfigValue("DRIVER_TYPE", "driver.type");
    String browser = getConfigValue("BROWSER", "browser");

    if (driverType == null) {
      if (browser != null) {
        driverType = browser;
      }
    }
    return driverType;
  }


  public static String getDevice() {
    String d = getConfigValue("device", "DEVICE");
    return (d != null) ? d :
      "desktop";
  }

  public static String getRetries() {
    String retries = getConfigValue("RETRIES", "retries");
    return (retries != null) ? retries :
      "0";
  }

  private static String getConfigValue(String... keys) {
    for (String key : keys) {
      String value = System.getProperty(key);
      if (value != null && !(value.isEmpty())) return value;

      value = System.getenv(key);
      if (value != null && !value.isEmpty()) return value;
    }
    return null;
  }
}
