package com.turnitin.playwright.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** A reusable, page-independent locator definition. */
public interface LocatorRef {

  Locator resolve(Page page);

  /**
   * Resolves this definition with additional selector options. Locator types
   * that do not use {@link Page.LocatorOptions} may keep the default behavior.
   */
  default Locator resolve(Page page, Page.LocatorOptions options) {
    return resolve(page);
  }

  String description();

  default LocatorRef first() {
    return LocatorRefs.first(this);
  }

  default LocatorRef last() {
    return LocatorRefs.last(this);
  }

  default LocatorRef nth(int index) {
    return LocatorRefs.nth(this, index);
  }

  default LocatorRef filter(Locator.FilterOptions options) {
    return LocatorRefs.filter(this, options);
  }

  default LocatorRef child(String selector) {
    return LocatorRefs.child(this, selector);
  }
}
