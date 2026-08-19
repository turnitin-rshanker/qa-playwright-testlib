package com.turnitin.playwright.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.turnitin.playwright.actions.PlaywrightActions;
import com.turnitin.playwright.config.FrameworkConfig;
import com.turnitin.playwright.config.ScreenshotMode;
import com.turnitin.playwright.config.TraceMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        .setArgs(Arrays.asList("--disable-popup-blocking",
                "--disable-notifications",
                "--disable-features=LocalNetworkAccessChecks"))

        .setHeadless(config.headless())
        .setSlowMo(config.slowMoMs());
    if (config.browser().channel() != null) {
      launchOptions.setChannel(config.browser().channel());
    }

    browser = browserType.launch(launchOptions);

    Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
            .setPermissions(Arrays.asList("clipboard-read",
                    "clipboard-write",
                    "camera",
                    "microphone"))
        .setViewportSize(config.viewportWidth(), config.viewportHeight())
        .setIgnoreHTTPSErrors(config.ignoreHttpsErrors());


    String currentDevice = config.device();
    if (currentDevice != null && !currentDevice.equalsIgnoreCase("desktop")) {
      // Device emulation - pulls viewport, user agent, touch support, etc.
      applyDeviceEmulation(contextOptions, currentDevice);
    }

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

  private void applyDeviceEmulation(Browser.NewContextOptions options, String device) {
    switch (device) {
      case "iPhone 18" -> options
              .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
              .setViewportSize(390, 844)
              .setDeviceScaleFactor(3)
              .setIsMobile(true)
              .setHasTouch(true);
      case "Pixel 9a" -> options
              .setUserAgent("Mozilla/5.0 (Linux; Android 11; Pixel 9a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
              .setViewportSize(393, 851)
              .setDeviceScaleFactor(2.75)
              .setIsMobile(true)
              .setHasTouch(true);
      default -> throw new IllegalArgumentException("Unknown device: " + device);
    }
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
