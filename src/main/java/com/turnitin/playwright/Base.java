package com.turnitin.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.turnitin.playwright.locators.LocatorRef;

/**
 * Base for pages that want an explicit loaded-state contract.
 *
 * <p>The constructor deliberately does not call {@link #loadableSelector()}.
 * Subclasses should initialize their locator fields first and then call
 * {@link #waitUntilLoaded()} from an {@code open()} or other initialization
 * method.</p>
 */
public abstract class Base extends Component {
  protected Base(Page page) {
    super(page);
  }

  /** Returns the locator that proves this page or component is ready. */
  protected abstract LocatorRef loadableSelector();

  /** Default loaded-state timeout in milliseconds. */
  protected double loadTimeoutMs() {
    return 30_000;
  }

  protected final void waitUntilLoaded() {
    waitUntilLoaded(loadableSelector(), loadTimeoutMs());
  }

  protected final void waitUntilLoaded(LocatorRef locatorRef) {
    waitUntilLoaded(locatorRef, loadTimeoutMs());
  }

  protected final void waitUntilLoaded(LocatorRef locatorRef, double timeoutMs) {
    Locator locator = findLocator(locatorRef);
    locator.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(requirePositiveTimeout(timeoutMs)));
  }

  private static double requirePositiveTimeout(double timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("timeoutMs must be greater than zero");
    }
    return timeoutMs;
  }
}
