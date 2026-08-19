package com.turnitin.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.turnitin.playwright.actions.BulkInputException;
import com.turnitin.playwright.actions.PlaywrightActions;
import com.turnitin.playwright.actions.TextFieldInput;
import com.turnitin.playwright.locators.LocatorRef;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reusable page/component facade backed by native Playwright locators.
 * Playwright auto-waiting and native exceptions remain intact.
 */
public class Component {
  private final Page page;
  private final PlaywrightActions actions;

  public Component(Page page) {
    this.page = Objects.requireNonNull(page, "page");
    this.actions = new PlaywrightActions(page);
  }

  public final Page getPage() {
    return page;
  }

  public final PlaywrightActions actions() {
    return actions;
  }

  public Locator findLocator(LocatorRef locatorRef) {
    return requireRef(locatorRef)
            .resolve(page);
  }

  /**
   * Resolves a locator while applying additional native
   * {@link Page.LocatorOptions}. The options are applied lazily to the current
   * page and flow through composed selector {@link LocatorRef} instances.
   * Role, label, text, and placeholder references use their strongly typed
   * options at creation time.
   */
  public Locator findLocator(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    Page.LocatorOptions options = locatorOptions(optionsCustomizer);
    return requireRef(locatorRef)
            .resolve(page, options);
  }

  public List<Locator> findLocators(LocatorRef locatorRef) {
    return findLocator(locatorRef).all();
  }

  public List<Locator> findLocators(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).all();
  }

  /** Accepts either a relative page URL or a complete HTTP(S) URL. */
  public Response navigate(String url) {
    return actions.navigate(url);
  }

  /** Navigates to a relative page URL resolved against the configured baseUrl. */
  public Response navigateToPage(String pageUrl) {
    return actions.navigateToPage(pageUrl);
  }

  /** Opens a complete HTTP or HTTPS URL without using the configured baseUrl. */
  public Response openUrl(String url) {
    return actions.openUrl(url);
  }

  public Component click(LocatorRef locatorRef) {
    findLocator(locatorRef).click();
    return this;
  }

  public Component click(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).click();
    return this;
  }

  public Component doubleClick(LocatorRef locatorRef) {
    findLocator(locatorRef).dblclick();
    return this;
  }

  public Component doubleClick(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).dblclick();
    return this;
  }

  public Component setValue(LocatorRef locatorRef, String value) {
    findLocator(locatorRef).fill(Objects.requireNonNull(value, "value"));
    return this;
  }

  public Component setValue(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String value) {
    findLocator(locatorRef, optionsCustomizer).fill(Objects.requireNonNull(value, "value"));
    return this;
  }

  public Component clearValue(LocatorRef locatorRef) {
    findLocator(locatorRef).clear();
    return this;
  }

  public Component clearValue(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).clear();
    return this;
  }

  public Component setValueAndEnter(LocatorRef locatorRef, String value) {
    Locator locator = findLocator(locatorRef);
    locator.fill(Objects.requireNonNull(value, "value"));
    locator.press("Enter");
    return this;
  }

  public Component setValueAndEnter(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String value) {
    Locator locator = findLocator(locatorRef, optionsCustomizer);
    locator.fill(Objects.requireNonNull(value, "value"));
    locator.press("Enter");
    return this;
  }

  public Component press(LocatorRef locatorRef, String key) {
    findLocator(locatorRef).press(requireText(key, "key"));
    return this;
  }

  public Component press(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String key) {
    findLocator(locatorRef, optionsCustomizer).press(requireText(key, "key"));
    return this;
  }

  public Component typeSequentially(LocatorRef locatorRef, String value) {
    findLocator(locatorRef).pressSequentially(Objects.requireNonNull(value, "value"));
    return this;
  }

  public Component typeSequentially(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String value) {
    findLocator(locatorRef, optionsCustomizer)
        .pressSequentially(Objects.requireNonNull(value, "value"));
    return this;
  }

  public Component check(LocatorRef locatorRef) {
    findLocator(locatorRef).check();
    return this;
  }

  public Component check(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).check();
    return this;
  }

  public Component uncheck(LocatorRef locatorRef) {
    findLocator(locatorRef).uncheck();
    return this;
  }

  public Component uncheck(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).uncheck();
    return this;
  }

  public Component hover(LocatorRef locatorRef) {
    findLocator(locatorRef).hover();
    return this;
  }

  public Component hover(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).hover();
    return this;
  }

  public Component focus(LocatorRef locatorRef) {
    findLocator(locatorRef).focus();
    return this;
  }

  public Component focus(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).focus();
    return this;
  }

  public List<String> selectByValue(LocatorRef locatorRef, String value) {
    return findLocator(locatorRef).selectOption(Objects.requireNonNull(value, "value"));
  }

  public List<String> selectByValue(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String value) {
    return findLocator(locatorRef, optionsCustomizer)
        .selectOption(Objects.requireNonNull(value, "value"));
  }

  public List<String> selectByLabel(LocatorRef locatorRef, String label) {
    return findLocator(locatorRef).selectOption(
        new SelectOption().setLabel(Objects.requireNonNull(label, "label")));
  }

  public List<String> selectByLabel(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String label) {
    return findLocator(locatorRef, optionsCustomizer).selectOption(
        new SelectOption().setLabel(Objects.requireNonNull(label, "label")));
  }

  public Component upload(LocatorRef locatorRef, Path... paths) {
    findLocator(locatorRef).setInputFiles(validateUploadPaths(paths));
    return this;
  }

  public Component upload(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      Path... paths) {
    findLocator(locatorRef, optionsCustomizer).setInputFiles(validateUploadPaths(paths));
    return this;
  }

  public Component dragTo(LocatorRef source, LocatorRef target) {
    findLocator(source).dragTo(findLocator(target));
    return this;
  }

  public Component dragTo(
      LocatorRef source,
      Consumer<Page.LocatorOptions> sourceOptionsCustomizer,
      LocatorRef target,
      Consumer<Page.LocatorOptions> targetOptionsCustomizer) {
    findLocator(source, sourceOptionsCustomizer)
        .dragTo(findLocator(target, targetOptionsCustomizer));
    return this;
  }

  public Component scrollIntoView(LocatorRef locatorRef) {
    findLocator(locatorRef).scrollIntoViewIfNeeded();
    return this;
  }

  public Component scrollIntoView(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    findLocator(locatorRef, optionsCustomizer).scrollIntoViewIfNeeded();
    return this;
  }

  public Component waitForAttached(LocatorRef locatorRef) {
    waitFor(findLocator(locatorRef), WaitForSelectorState.ATTACHED);
    return this;
  }

  public Component waitForAttached(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    waitFor(findLocator(locatorRef, optionsCustomizer), WaitForSelectorState.ATTACHED);
    return this;
  }

  public Component waitForDetached(LocatorRef locatorRef) {
    waitFor(findLocator(locatorRef), WaitForSelectorState.DETACHED);
    return this;
  }

  public Component waitForDetached(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    waitFor(findLocator(locatorRef, optionsCustomizer), WaitForSelectorState.DETACHED);
    return this;
  }

  /** Fills an ordered list of edit fields and stops at the first failure. */
  public Component setValues(List<TextFieldInput> fields) {
    List<TextFieldInput> safeFields = validateFields(fields);
    for (int index = 0; index < safeFields.size(); index++) {
      TextFieldInput input = safeFields.get(index);
      try {
        findLocator(input.field()).fill(input.value());
      } catch (RuntimeException exception) {
        throw new BulkInputException(index, input.field().description(), exception);
      }
    }
    return this;
  }

  public Component setValues(TextFieldInput... fields) {
    Objects.requireNonNull(fields, "fields");
    return setValues(Arrays.asList(fields.clone()));
  }

  public String inputValue(LocatorRef locatorRef) {
    return findLocator(locatorRef).inputValue();
  }

  public String inputValue(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).inputValue();
  }

  public String getText(LocatorRef locatorRef) {
    return findLocator(locatorRef).textContent();
  }

  public String getText(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).textContent();
  }

  public String getAttribute(LocatorRef locatorRef, String name) {
    return findLocator(locatorRef).getAttribute(requireText(name, "attribute name"));
  }

  public String getAttribute(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer,
      String name) {
    return findLocator(locatorRef, optionsCustomizer)
        .getAttribute(requireText(name, "attribute name"));
  }

  public List<String> getAllText(LocatorRef locatorRef) {
    return findLocator(locatorRef).allTextContents();
  }

  public List<String> getAllText(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).allTextContents();
  }

  public int getElementCount(LocatorRef locatorRef) {
    return findLocator(locatorRef).count();
  }

  public int getElementCount(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).count();
  }

  public boolean isPresent(LocatorRef locatorRef) {
    return getElementCount(locatorRef) > 0;
  }

  public boolean isPresent(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return getElementCount(locatorRef, optionsCustomizer) > 0;
  }

  public boolean isVisible(LocatorRef locatorRef) {
    return findLocator(locatorRef).isVisible();
  }

  public boolean isVisible(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).isVisible();
  }

  public boolean isEnabled(LocatorRef locatorRef) {
    return findLocator(locatorRef).isEnabled();
  }

  public boolean isEnabled(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).isEnabled();
  }

  public boolean isChecked(LocatorRef locatorRef) {
    return findLocator(locatorRef).isChecked();
  }

  public boolean isChecked(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    return findLocator(locatorRef, optionsCustomizer).isChecked();
  }

  protected final void waitUntilVisible(Locator locator) {
    requireLocator(locator).waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE));
  }

  public Component waitUntilVisible(LocatorRef locatorRef) {
    waitUntilVisible(findLocator(locatorRef));
    return this;
  }

  public Component waitUntilVisible(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    waitUntilVisible(findLocator(locatorRef, optionsCustomizer));
    return this;
  }

  public Component waitUntilHidden(LocatorRef locatorRef) {
    waitUntilHidden(findLocator(locatorRef));
    return this;
  }

  public Component waitUntilHidden(
      LocatorRef locatorRef,
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    waitUntilHidden(findLocator(locatorRef, optionsCustomizer));
    return this;
  }

  private static void waitUntilHidden(Locator locator) {
    waitFor(locator, WaitForSelectorState.HIDDEN);
  }

  private static void waitFor(Locator locator, WaitForSelectorState state) {
    requireLocator(locator).waitFor(new Locator.WaitForOptions()
        .setState(Objects.requireNonNull(state, "state")));
  }

  private static Path[] validateUploadPaths(Path[] paths) {
    Objects.requireNonNull(paths, "paths");
    if (paths.length == 0) {
      throw new IllegalArgumentException("at least one upload path is required");
    }
    Path[] safePaths = paths.clone();
    for (int index = 0; index < safePaths.length; index++) {
      if (safePaths[index] == null) {
        throw new IllegalArgumentException("paths[" + index + "] must not be null");
      }
    }
    return safePaths;
  }

  private static List<TextFieldInput> validateFields(List<TextFieldInput> fields) {
    Objects.requireNonNull(fields, "fields");
    List<TextFieldInput> safeFields = new ArrayList<>(fields.size());
    for (int index = 0; index < fields.size(); index++) {
      TextFieldInput field = fields.get(index);
      if (field == null) {
        throw new IllegalArgumentException("fields[" + index + "] must not be null");
      }
      safeFields.add(field);
    }
    return List.copyOf(safeFields);
  }

  private static Page.LocatorOptions locatorOptions(
      Consumer<Page.LocatorOptions> optionsCustomizer) {
    Page.LocatorOptions options = new Page.LocatorOptions();
    Objects.requireNonNull(optionsCustomizer, "optionsCustomizer").accept(options);
    return options;
  }

  private static LocatorRef requireRef(LocatorRef locatorRef) {
    return Objects.requireNonNull(locatorRef, "locatorRef");
  }

  private static Locator requireLocator(Locator locator) {
    return Objects.requireNonNull(locator, "locator");
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
