package dev.codex.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import dev.codex.playwright.actions.PlaywrightActions;
import dev.codex.playwright.testng.PlaywrightTestContext;
import dev.codex.playwright.testng.listeners.PlaywrightTestListener;

/**
 * Minimal base for TestNG browser tests.
 *
 * <p>{@link PlaywrightTestListener} owns the complete test lifecycle. The
 * session is available during the {@code @Test} method; configuration methods
 * should not call {@link #getPage()}.</p>
 */
public abstract class BaseTest {

  protected final Page getPage() {
    return PlaywrightTestContext.currentPage();
  }

  /** Returns the non-owning action facade for the current listener-managed page. */
  protected final PlaywrightActions actions() {
    return PlaywrightTestContext.currentActions();
  }

  /**
   * Backward-compatible navigation accepting either a relative page URL or a
   * complete HTTP(S) URL.
   */
  public final Response navigate(String url) {
    return actions().navigate(url);
  }

  /** Explicit relative-page navigation method. */
  public final Response navigateToPage(String path) {
    return actions().navigateToPage(path);
  }

  /** Opens a complete HTTP or HTTPS URL without using the configured baseUrl. */
  public final Response openUrl(String url) {
    return actions().openUrl(url);
  }
}
