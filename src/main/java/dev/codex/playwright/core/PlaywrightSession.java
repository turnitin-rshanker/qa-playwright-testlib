package dev.codex.playwright.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import dev.codex.playwright.actions.PlaywrightActions;
import dev.codex.playwright.config.FrameworkConfig;
import dev.codex.playwright.config.ScreenshotMode;
import dev.codex.playwright.config.TraceMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlaywrightSession implements AutoCloseable {
  private static final DateTimeFormatter ARTIFACT_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

  private final FrameworkConfig config;
  private final Path artifactDirectory;
  private final AtomicBoolean closed = new AtomicBoolean();
  private final List<Path> artifacts = new ArrayList<>();
  private final Deque<AutoCloseable> managedResources = new ArrayDeque<>();

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private Page page;
  private PlaywrightActions actions;
  private BrowserDiagnostics diagnostics;
  private boolean tracingStarted;

  private PlaywrightSession(FrameworkConfig config, String testId) {
    this.config = Objects.requireNonNull(config, "config");
    this.artifactDirectory = config.artifactsDirectory()
        .resolve(safeName(testId))
        .resolve(ARTIFACT_TIME.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8));
  }

  public static PlaywrightSession start(FrameworkConfig config, String testId) {
    PlaywrightSession session = new PlaywrightSession(config, testId);
    try {
      session.initialize();
      return session;
    } catch (RuntimeException | Error failure) {
      session.close(true, failure);
      throw failure;
    }
  }

  public Page page() {
    ensureOpen();
    return page;
  }

  /** Returns the non-owning action facade for the listener-managed page. */
  public PlaywrightActions actions() {
    ensureOpen();
    return actions;
  }

  public BrowserContext context() {
    ensureOpen();
    return context;
  }

  public Path artifactDirectory() {
    return artifactDirectory;
  }

  public ArtifactBundle close(boolean failed, Throwable failure) {
    if (!closed.compareAndSet(false, true)) {
      return snapshotArtifacts();
    }

    if (diagnostics != null) {
      diagnostics.recordFailure(failure);
    }

    captureScreenshot(failed);
    stopTracing(failed);
    writeDiagnostics();

    closeManagedResources();
    safelyClose("browser context", context);
    safelyClose("browser", browser);
    safelyClose("Playwright", playwright);
    context = null;
    browser = null;
    playwright = null;
    page = null;
    actions = null;

    return snapshotArtifacts();
  }

  @Override
  public void close() {
    close(false, null);
  }

  private void initialize() {
    try {
      Files.createDirectories(artifactDirectory);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create artifact directory " + artifactDirectory, exception);
    }

    playwright = Playwright.create();
    BrowserType browserType = switch (config.browser()) {
      case CHROMIUM, CHROME, MSEDGE -> playwright.chromium();
      case FIREFOX -> playwright.firefox();
      case WEBKIT -> playwright.webkit();
    };

    BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
        .setHeadless(config.headless())
        .setSlowMo(config.slowMoMs());
    if (config.browser().channel() != null) {
      launchOptions.setChannel(config.browser().channel());
    }
    browser = browserType.launch(launchOptions);

    Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
        .setViewportSize(config.viewportWidth(), config.viewportHeight())
        .setIgnoreHTTPSErrors(config.ignoreHttpsErrors());
    if (!config.baseUrl().isBlank()) {
      contextOptions.setBaseURL(config.baseUrl());
    }
    context = browser.newContext(contextOptions);
    context.setDefaultTimeout(config.actionTimeoutMs());
    context.setDefaultNavigationTimeout(config.navigationTimeoutMs());

    if (config.diagnosticsEnabled()) {
      diagnostics = new BrowserDiagnostics();
      diagnostics.attach(context);
    }

    if (config.traceMode() != TraceMode.OFF) {
      context.tracing().start(new Tracing.StartOptions()
          .setScreenshots(true)
          .setSnapshots(true)
          .setSources(true));
      tracingStarted = true;
    }

    page = context.newPage();
    actions = new PlaywrightActions(page, this::manage, config.baseUrl());
  }

  private void manage(AutoCloseable resource) {
    AutoCloseable safeResource = Objects.requireNonNull(resource, "resource");
    if (closed.get()) {
      safelyClose("late page resource", safeResource);
      throw new IllegalStateException("Cannot register a page resource after the session is closed");
    }
    managedResources.addFirst(safeResource);
  }

  private void closeManagedResources() {
    while (!managedResources.isEmpty()) {
      safelyClose("managed page resource", managedResources.removeFirst());
    }
  }

  private void captureScreenshot(boolean failed) {
    boolean shouldCapture = config.screenshotMode() == ScreenshotMode.ON
        || (config.screenshotMode() == ScreenshotMode.ONLY_ON_FAILURE && failed);
    if (!shouldCapture || page == null || page.isClosed()) {
      return;
    }

    Path path = artifactDirectory.resolve("screenshot.png");
    try {
      page.screenshot(new Page.ScreenshotOptions().setPath(path).setFullPage(true));
      artifacts.add(path);
    } catch (RuntimeException exception) {
      reportArtifactProblem("screenshot", exception);
    }
  }

  private void stopTracing(boolean failed) {
    if (!tracingStarted || context == null) {
      return;
    }

    boolean shouldKeep = config.traceMode() == TraceMode.ON
        || (config.traceMode() == TraceMode.RETAIN_ON_FAILURE && failed);
    try {
      if (shouldKeep) {
        Path path = artifactDirectory.resolve("trace.zip");
        context.tracing().stop(new Tracing.StopOptions().setPath(path));
        artifacts.add(path);
      } else {
        context.tracing().stop();
      }
    } catch (RuntimeException exception) {
      reportArtifactProblem("trace", exception);
    } finally {
      tracingStarted = false;
    }
  }

  private void writeDiagnostics() {
    if (diagnostics == null) {
      return;
    }
    try {
      artifacts.addAll(diagnostics.write(artifactDirectory));
    } catch (RuntimeException exception) {
      reportArtifactProblem("browser diagnostics", exception);
    }
  }

  private ArtifactBundle snapshotArtifacts() {
    return new ArtifactBundle(artifactDirectory, artifacts);
  }

  private void ensureOpen() {
    if (closed.get() || page == null) {
      throw new IllegalStateException("Playwright session is not open");
    }
  }

  private static String safeName(String value) {
    String safe = value == null ? "test" : value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    return safe.isBlank() ? "test" : safe;
  }

  private static void safelyClose(String resourceName, AutoCloseable resource) {
    if (resource == null) {
      return;
    }
    try {
      resource.close();
    } catch (Exception exception) {
      System.err.println("Could not close " + resourceName + ": " + exception.getMessage());
    }
  }

  private static void reportArtifactProblem(String artifactName, Exception exception) {
    System.err.println("Could not capture " + artifactName + ": " + exception.getMessage());
  }
}
