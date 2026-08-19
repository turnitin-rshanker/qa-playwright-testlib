package dev.codex.playwright.actions;

import com.microsoft.playwright.Locator;
import dev.codex.playwright.locators.LocatorRef;
import dev.codex.playwright.locators.LocatorRefs;
import java.util.Objects;

/** One ordered text-field assignment for {@link dev.codex.playwright.Component#setValues}. */
public record TextFieldInput(LocatorRef field, String value) {
  public TextFieldInput {
    Objects.requireNonNull(field, "field");
    Objects.requireNonNull(value, "value");
  }

  public static TextFieldInput of(LocatorRef field, String value) {
    return new TextFieldInput(field, value);
  }

  public static TextFieldInput of(Locator locator, String description, String value) {
    return new TextFieldInput(LocatorRefs.fixed(locator, description), value);
  }

  @Override
  public String toString() {
    return "TextFieldInput[field=" + field.description() + ", value=<redacted>]";
  }
}
