package dev.codex.playwright.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Factory methods for option-aware, composable Playwright locators. */
public final class LocatorRefs {
  private LocatorRefs() {
  }

  public static LocatorRef selector(String selector) {
    String safeSelector = requireText(selector, "selector");
    return defined(
        "selector=" + safeSelector,
        (page, additionalOptions) -> page.locator(safeSelector, copy(additionalOptions)));
  }

  public static LocatorRef selector(String selector, Page.LocatorOptions options) {
    String safeSelector = requireText(selector, "selector");
    Objects.requireNonNull(options, "options");
    Page.LocatorOptions fixedOptions = copy(options);
    return defined(
        "selector=" + safeSelector + " with options",
        (page, additionalOptions) -> page.locator(
            safeSelector,
            merge(fixedOptions, additionalOptions)));
  }

  public static LocatorRef role(AriaRole role) {
    Objects.requireNonNull(role, "role");
    return defined("role=" + role, page -> page.getByRole(role));
  }

  public static LocatorRef role(AriaRole role, Page.GetByRoleOptions options) {
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(options, "options");
    return defined("role=" + role + " with options", page -> page.getByRole(role, options));
  }

  public static LocatorRef label(String label) {
    String safeLabel = requireText(label, "label");
    return defined("label=" + safeLabel, page -> page.getByLabel(safeLabel));
  }

  public static LocatorRef label(String label, Page.GetByLabelOptions options) {
    String safeLabel = requireText(label, "label");
    Objects.requireNonNull(options, "options");
    return defined("label=" + safeLabel + " with options", page -> page.getByLabel(safeLabel, options));
  }

//  public static LocatorRef placeholder(String placeholder) {
//    String safePlaceholder = requireText(placeholder, "placeholder");
//    return defined("placeholder=" + safePlaceholder, page -> page.getByPlaceholder(safePlaceholder));
//  }
//
//  public static LocatorRef placeholder(String placeholder, Page.GetByPlaceholderOptions options) {
//    String safePlaceholder = requireText(placeholder, "placeholder");
//    Objects.requireNonNull(options, "options");
//    return defined(
//        "placeholder=" + safePlaceholder + " with options",
//        page -> page.getByPlaceholder(safePlaceholder, options));
//  }
//
//  public static LocatorRef text(String text) {
//    String safeText = requireText(text, "text");
//    return defined("text=" + safeText, page -> page.getByText(safeText));
//  }
//
//  public static LocatorRef text(String text, Page.GetByTextOptions options) {
//    String safeText = requireText(text, "text");
//    Objects.requireNonNull(options, "options");
//    return defined("text=" + safeText + " with options", page -> page.getByText(safeText, options));
//  }
//
//  public static LocatorRef testId(String testId) {
//    String safeTestId = requireText(testId, "testId");
//    return defined("testId=" + safeTestId, page -> page.getByTestId(safeTestId));
//  }

  public static LocatorRef fixed(Locator locator, String description) {
    Locator safeLocator = Objects.requireNonNull(locator, "locator");
    return defined(requireText(description, "description"), page -> safeLocator);
  }

  public static LocatorRef first(LocatorRef source) {
    LocatorRef safeSource = requireRef(source);
    return defined(
        safeSource.description() + ".first()",
        (page, options) -> safeSource.resolve(page, options).first());
  }

  public static LocatorRef last(LocatorRef source) {
    LocatorRef safeSource = requireRef(source);
    return defined(
        safeSource.description() + ".last()",
        (page, options) -> safeSource.resolve(page, options).last());
  }

  public static LocatorRef nth(LocatorRef source, int index) {
    LocatorRef safeSource = requireRef(source);
    if (index < 0) {
      throw new IllegalArgumentException("index must be zero or greater");
    }
    return defined(
        safeSource.description() + ".nth(" + index + ")",
        (page, options) -> safeSource.resolve(page, options).nth(index));
  }

  public static LocatorRef filter(LocatorRef source, Locator.FilterOptions options) {
    LocatorRef safeSource = requireRef(source);
    Objects.requireNonNull(options, "options");
    return defined(
        safeSource.description() + ".filter(options)",
        (page, locatorOptions) -> safeSource.resolve(page, locatorOptions).filter(options));
  }

  public static LocatorRef child(LocatorRef parent, String selector) {
    LocatorRef safeParent = requireRef(parent);
    String safeSelector = requireText(selector, "selector");
    return defined(
        safeParent.description() + ".locator(" + safeSelector + ")",
        (page, options) -> safeParent.resolve(page, options).locator(safeSelector));
  }

  private static LocatorRef defined(String description, Function<Page, Locator> resolver) {
    Objects.requireNonNull(resolver, "resolver");
    return new DefinedLocatorRef(description, (page, options) -> resolver.apply(page));
  }

  private static LocatorRef defined(
      String description,
      BiFunction<Page, Page.LocatorOptions, Locator> resolver) {
    return new DefinedLocatorRef(description, resolver);
  }

  private static Page.LocatorOptions copy(Page.LocatorOptions source) {
    Page.LocatorOptions copy = new Page.LocatorOptions();
    if (source == null) {
      return copy;
    }
    copy.has = source.has;
    copy.hasNot = source.hasNot;
    copy.hasText = source.hasText;
    copy.hasNotText = source.hasNotText;
    return copy;
  }

  private static Page.LocatorOptions merge(
      Page.LocatorOptions fixedOptions,
      Page.LocatorOptions additionalOptions) {
    Page.LocatorOptions merged = copy(fixedOptions);
    if (additionalOptions == null) {
      return merged;
    }
    if (additionalOptions.has != null) {
      merged.has = additionalOptions.has;
    }
    if (additionalOptions.hasNot != null) {
      merged.hasNot = additionalOptions.hasNot;
    }
    if (additionalOptions.hasText != null) {
      merged.hasText = additionalOptions.hasText;
    }
    if (additionalOptions.hasNotText != null) {
      merged.hasNotText = additionalOptions.hasNotText;
    }
    return merged;
  }

  private static LocatorRef requireRef(LocatorRef locatorRef) {
    return Objects.requireNonNull(locatorRef, "locatorRef");
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  private record DefinedLocatorRef(
      String description,
      BiFunction<Page, Page.LocatorOptions, Locator> resolver) implements LocatorRef {

    private DefinedLocatorRef {
      requireText(description, "description");
      Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public Locator resolve(Page page) {
      return resolve(page, new Page.LocatorOptions());
    }

    @Override
    public Locator resolve(Page page, Page.LocatorOptions options) {
      return Objects.requireNonNull(
          resolver.apply(
              Objects.requireNonNull(page, "page"),
              copy(Objects.requireNonNull(options, "options"))),
          "resolved locator");
    }

    @Override
    public String toString() {
      return description;
    }
  }
}
