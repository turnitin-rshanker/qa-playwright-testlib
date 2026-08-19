package dev.codex.playwright.testng;

import com.microsoft.playwright.Page;
import dev.codex.playwright.actions.PlaywrightActions;
import dev.codex.playwright.config.FrameworkConfig;
import dev.codex.playwright.core.ArtifactBundle;
import dev.codex.playwright.core.PlaywrightSession;
import io.qameta.allure.Allure;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.testng.ITestResult;

/** Thread-confined session storage used by the TestNG listener and BaseTest. */
public final class PlaywrightTestContext {
  private static final ThreadLocal<ManagedSession> CURRENT = new ThreadLocal<>();

  private PlaywrightTestContext() {
  }

  public static Page currentPage() {
    return requireCurrent().page();
  }

  /** Returns actions bound to the current listener-owned page without transferring ownership. */
  public static PlaywrightActions currentActions() {
    return requireCurrent().actions();
  }

  private static ManagedSession requireCurrent() {
    ManagedSession managedSession = CURRENT.get();
    if (managedSession == null) {
      throw new IllegalStateException(
          "No Playwright session is active. page() is available only during a TestNG @Test method.");
    }
    return managedSession;
  }

  public static void start(ITestResult result) {
    if (CURRENT.get() != null) {
      throw new IllegalStateException("A Playwright session is already active on this test thread");
    }

    FrameworkConfig config = FrameworkConfig.load();
    String browserName = config.browser().name().toLowerCase(Locale.ROOT);
    String testId = result.getTestClass().getRealClass().getSimpleName()
        + "." + result.getMethod().getMethodName()
        + "." + config.environment()
        + "." + browserName;

    CURRENT.set(openManagedSession(config, testId));
    try {
      Allure.parameter("environment", config.environment());
      Allure.parameter("browser", browserName);
    } catch (RuntimeException exception) {
      System.err.println(
          "Could not add the Allure environment or browser parameter; the test will continue: "
              + exception.getMessage());
    }
  }

  public static void closeAndAttach(ITestResult result) {
    ManagedSession managedSession = CURRENT.get();
    try {
      if (managedSession == null) {
        return;
      }

      boolean failed = result.getThrowable() != null || result.getStatus() == ITestResult.FAILURE;
      ArtifactBundle bundle = managedSession.close(failed, result.getThrowable());
      attachToAllure(bundle);
    } catch (RuntimeException exception) {
      System.err.println(
          "Could not complete Playwright reporting; the test result is unchanged: "
              + exception.getMessage());
    } finally {
      CURRENT.remove();
    }
  }

  /** Ownership transfers immediately to ManagedSession and is released by closeAndAttach. */
  private static ManagedSession openManagedSession(FrameworkConfig config, String testId) {
    return new ManagedSession(PlaywrightSession.start(config, testId));
  }

  private static void attachToAllure(ArtifactBundle bundle) {
    for (Path path : bundle.files()) {
      if (!Files.isRegularFile(path)) {
        continue;
      }
      try (InputStream input = Files.newInputStream(path)) {
        String extension = extension(path);
        Allure.addAttachment(path.getFileName().toString(), contentType(extension), input, extension);
      } catch (IOException | RuntimeException exception) {
        System.err.println(
            "Could not attach Allure artifact " + path + ": " + exception.getMessage());
      }
    }
  }

  private static String extension(Path path) {
    String name = path.getFileName().toString();
    int dot = name.lastIndexOf('.');
    return dot >= 0 ? name.substring(dot + 1) : "txt";
  }

  private static String contentType(String extension) {
    return switch (extension) {
      case "png" -> "image/png";
      case "zip" -> "application/zip";
      case "json" -> "application/json";
      case "jsonl" -> "application/x-ndjson";
      default -> "text/plain";
    };
  }

  /**
   * Non-AutoCloseable holder prevents managed sessions from escaping to test code.
   */
  private record ManagedSession(PlaywrightSession session) {

    private Page page() {
      return session.page();
    }

    private PlaywrightActions actions() {
      return session.actions();
    }

    private ArtifactBundle close(boolean failed, Throwable failure) {
      return session.close(failed, failure);
    }
  }
}
