package com.turnitin.playwright.actions;

import com.microsoft.playwright.Locator;
import com.turnitin.playwright.locators.LocatorRef;
import com.turnitin.playwright.locators.LocatorRefs;
import java.util.Objects;

/** One ordered text-field assignment for {@link com.turnitin.playwright.Component#setValues}. */
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
