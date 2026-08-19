package dev.codex.playwright.actions;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.LoadState;
import dev.codex.playwright.util.NavigationUrls;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class PageActions {
  private static final System.Logger LOGGER = System.getLogger(PageActions.class.getName());

  private final Page page;
  private final Consumer<AutoCloseable> resourceRegistrar;
  private final String baseUrl;

  PageActions(
      Page page,
      Consumer<AutoCloseable> resourceRegistrar,
      String baseUrl) {
    this.page = Objects.requireNonNull(page, "page");
    this.resourceRegistrar = Objects.requireNonNull(resourceRegistrar, "resourceRegistrar");
    this.baseUrl = baseUrl;
  }

  Response navigate(String url) {
    String target = NavigationUrls.requireNavigationTarget(url);
    if (!URI.create(target).isAbsolute()) {
      logResolvedNavigation(target);
    }
    return page.navigate(target);
  }

  Response navigateToPage(String pageUrl) {
    String safePageUrl = NavigationUrls.requirePageUrl(pageUrl);
    logResolvedNavigation(safePageUrl);
    return page.navigate(safePageUrl);
  }

  Response openUrl(String url) {
    return page.navigate(NavigationUrls.requireAbsoluteUrl(url));
  }

  void route(String urlPattern, Consumer<Route> handler) {
    register(page.route(requireText(urlPattern, "urlPattern"), requireHandler(handler)));
  }

  void route(String urlPattern, Consumer<Route> handler, Page.RouteOptions options) {
    register(page.route(
        requireText(urlPattern, "urlPattern"),
        requireHandler(handler),
        Objects.requireNonNull(options, "options")));
  }

  void route(Pattern urlPattern, Consumer<Route> handler) {
    register(page.route(Objects.requireNonNull(urlPattern, "urlPattern"), requireHandler(handler)));
  }

  void route(Pattern urlPattern, Consumer<Route> handler, Page.RouteOptions options) {
    register(page.route(
        Objects.requireNonNull(urlPattern, "urlPattern"),
        requireHandler(handler),
        Objects.requireNonNull(options, "options")));
  }

  void route(Predicate<String> urlPredicate, Consumer<Route> handler) {
    register(page.route(Objects.requireNonNull(urlPredicate, "urlPredicate"), requireHandler(handler)));
  }

  void route(
      Predicate<String> urlPredicate,
      Consumer<Route> handler,
      Page.RouteOptions options) {
    register(page.route(
        Objects.requireNonNull(urlPredicate, "urlPredicate"),
        requireHandler(handler),
        Objects.requireNonNull(options, "options")));
  }

  Response reload() {
    return page.reload();
  }

  Response goBack() {
    return page.goBack();
  }

  Response goForward() {
    return page.goForward();
  }

  void setContent(String html) {
    page.setContent(Objects.requireNonNull(html, "html"));
  }

  void waitForUrl(String urlPattern) {
    page.waitForURL(requireText(urlPattern, "urlPattern"));
  }

  void waitForLoadState(LoadState state) {
    page.waitForLoadState(Objects.requireNonNull(state, "state"));
  }

  String title() {
    return page.title();
  }

  String url() {
    return page.url();
  }

  private void logResolvedNavigation(String pageUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      LOGGER.log(
          System.Logger.Level.INFO,
          "Navigating: path={0}; BrowserContext will resolve it against its configured baseUrl",
          NavigationUrls.safeForLog(pageUrl));
      return;
    }

    String resolvedUrl = NavigationUrls.resolvePageUrl(baseUrl, pageUrl);
    LOGGER.log(
        System.Logger.Level.INFO,
        "Navigating: baseUrl={0}, path={1}, resolvedUrl={2}",
        NavigationUrls.safeForLog(baseUrl),
        NavigationUrls.safeForLog(pageUrl),
        NavigationUrls.safeForLog(resolvedUrl));
  }

  private void register(AutoCloseable registration) {
    AutoCloseable safeRegistration = Objects.requireNonNull(registration, "route registration");
    try {
      resourceRegistrar.accept(safeRegistration);
    } catch (RuntimeException | Error failure) {
      try {
        safeRegistration.close();
      } catch (Exception closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private static Consumer<Route> requireHandler(Consumer<Route> handler) {
    return Objects.requireNonNull(handler, "handler");
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
