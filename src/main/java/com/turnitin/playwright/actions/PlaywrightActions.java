package com.turnitin.playwright.actions;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.LoadState;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Page-level operations and listener-managed route registrations.
 * Locator interactions belong to {@code Component} and accept {@code LocatorRef}.
 */
public final class PlaywrightActions {

  private final PageActions pageActions;

  public PlaywrightActions(Page page) {
    this(page, ignored -> {}, null);
  }

  /** Framework constructor that transfers returned page resources to the session registry. */
  public PlaywrightActions(Page page, Consumer<AutoCloseable> resourceRegistrar) {
    this(page, resourceRegistrar, null);
  }

  /** Framework constructor with listener-owned resources and configured base URL. */
  public PlaywrightActions(
      Page page,
      Consumer<AutoCloseable> resourceRegistrar,
      String baseUrl) {
    this.pageActions = new PageActions(page, resourceRegistrar, baseUrl);
  }

  public Response navigate(String url) {
    return pageActions.navigate(url);
  }

  /** Navigates to a relative page URL resolved against the configured baseUrl. */
  public Response navigateToPage(String pageUrl) {
    return pageActions.navigateToPage(pageUrl);
  }

  /** Opens a complete HTTP or HTTPS URL without using the configured baseUrl. */
  public Response openUrl(String url) {
    return pageActions.openUrl(url);
  }

  /** Registers a route for the page lifetime; listener-owned actions close it during teardown. */
  public PlaywrightActions route(String urlPattern, Consumer<Route> handler) {
    pageActions.route(urlPattern, handler);
    return this;
  }

  public PlaywrightActions route(
      String urlPattern,
      Consumer<Route> handler,
      Page.RouteOptions options) {
    pageActions.route(urlPattern, handler, options);
    return this;
  }

  public PlaywrightActions route(
      Pattern urlPattern,
      Consumer<Route> handler) {
    pageActions.route(urlPattern, handler);
    return this;
  }

  public PlaywrightActions route(
      Pattern urlPattern,
      Consumer<Route> handler,
      Page.RouteOptions options) {
    pageActions.route(urlPattern, handler, options);
    return this;
  }

  public PlaywrightActions route(
      Predicate<String> urlPredicate,
      Consumer<Route> handler) {
    pageActions.route(urlPredicate, handler);
    return this;
  }

  public PlaywrightActions route(
      Predicate<String> urlPredicate,
      Consumer<Route> handler,
      Page.RouteOptions options) {
    pageActions.route(urlPredicate, handler, options);
    return this;
  }

  public Response reload() {
    return pageActions.reload();
  }

  public Response goBack() {
    return pageActions.goBack();
  }

  public Response goForward() {
    return pageActions.goForward();
  }

  public PlaywrightActions setContent(String html) {
    pageActions.setContent(html);
    return this;
  }

  public PlaywrightActions waitForUrl(String urlPattern) {
    pageActions.waitForUrl(urlPattern);
    return this;
  }

  public PlaywrightActions waitForLoadState(LoadState state) {
    pageActions.waitForLoadState(state);
    return this;
  }

  public String title() {
    return pageActions.title();
  }

  public String url() {
    return pageActions.url();
  }
}
