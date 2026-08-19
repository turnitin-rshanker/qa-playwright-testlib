package com.turnitin.playwright.testng;

import com.turnitin.playwright.config.FrameworkConfig;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/** Writes one truthful Allure environment file for a TestNG suite. */
public final class AllureEnvWriter implements ISuiteListener {
  private static final Object WRITE_LOCK = new Object();
  private static boolean written;

  @Override
  public void onStart(ISuite suite) {
    synchronized (WRITE_LOCK) {
      if (written) {
        return;
      }
      Objects.requireNonNull(suite, "suite");
      if (writeEnvironment()) {
        written = true;
      }
    }
  }

  private static boolean writeEnvironment() {
    Path resultsDirectory = Path.of(
    System.getProperty("allure.results.directory", "target/allure-results"));
    Path target = resultsDirectory.resolve("environment.properties");
    Path temporary = resultsDirectory.resolve("environment.properties.tmp");

    try {
      FrameworkConfig config = FrameworkConfig.load();
      Properties environment = new Properties();
      environment.setProperty("environment", config.environment());
      environment.setProperty("browser", config.browser().name().toLowerCase());
      environment.setProperty("headless", Boolean.toString(config.headless()));
      environment.setProperty("os", System.getProperty("os.name", "unknown"));
      environment.setProperty("java.version", System.getProperty("java.version", "unknown"));

      Files.createDirectories(resultsDirectory);
      try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
        environment.store(writer, "Java Playwright test environment");
      }
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (RuntimeException | IOException exception) {
      System.err.println(
          "Could not write Allure environment.properties; tests will continue: "
              + exception.getMessage());
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        // Best-effort cleanup must never affect the suite.
      }
      return false;
    }
  }

}
