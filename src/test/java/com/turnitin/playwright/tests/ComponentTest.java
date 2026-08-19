package com.turnitin.playwright.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.turnitin.playwright.Base;
import com.turnitin.playwright.actions.TextFieldInput;
import com.turnitin.playwright.locators.LocatorRef;
import com.turnitin.playwright.locators.LocatorRefs;
import java.util.List;
import org.testng.annotations.Test;

public final class ComponentTest extends UnitBaseTest {

  @Test
  public void appliesAdditionalLocatorOptionsThroughComposedReferences() {
    AccountForm form = new AccountForm(getPage()).open();

    form.setValue(
        AccountForm.ACTIVE_ROW.child("input"),
        options -> options.setHasNotText("Archived"),
        "secondary@example.test");

    assertThat(form.findLocators(
        AccountForm.ROWS,
        options -> options.setHasText("Secondary")))
        .hasSize(2);
    assertThat(form.inputValue(
        AccountForm.ACTIVE_ROW.child("input"),
        options -> options.setHasNotText("Archived")))
        .isEqualTo("secondary@example.test");
  }

  @Test
  public void fillsAnOrderedListAndWaitsOnlyAfterSubclassInitialization() {
    AccountForm form = new AccountForm(getPage()).open();

    form.setValues(List.of(
        TextFieldInput.of(LocatorRefs.label("First name"), "Ada"),
        TextFieldInput.of(LocatorRefs.label("Last name"), "Lovelace")));

    assertThat(form.inputValue(LocatorRefs.label("First name"))).isEqualTo("Ada");
    assertThat(form.inputValue(LocatorRefs.label("Last name"))).isEqualTo("Lovelace");
  }

  @Test
  public void exposesCommonLocatorActionsAsAFluentComponentApi() {
    AccountForm form = new AccountForm(getPage()).openWithControls();
    LocatorRef command = LocatorRefs.selector("#command");
    LocatorRef choiceRows = LocatorRefs.selector(".choice-row");
    LocatorRef choiceInput = choiceRows.child("input");
    LocatorRef doubleButton = LocatorRefs.selector("#double-button");
    LocatorRef status = LocatorRefs.selector("#status");
    LocatorRef messages = LocatorRefs.selector(".message");

    form
        .setValue(command, "draft")
        .focus(command)
        .clearValue(command)
        .setValueAndEnter(command, "submitted")
        .press(command, "End")
        .check(choiceInput, options -> options.setHasText("Preferred"))
        .doubleClick(doubleButton)
        .waitUntilVisible(status)
        .hover(status);

    assertThat(form.inputValue(command)).isEqualTo("submitted");
    assertThat(form.getAttribute(command, "data-submitted")).isEqualTo("submitted");
    assertThat(form.isChecked(
        choiceInput,
        options -> options.setHasText("Preferred")))
        .isTrue();
    assertThat(form.isEnabled(
        choiceInput,
        options -> options.setHasText("Disabled")))
        .isFalse();
    assertThat(form.isVisible(status)).isTrue();
    assertThat(form.getText(status)).isEqualTo("Ready");
    assertThat(form.getText(
        messages,
        options -> options.setHasText("Two")))
        .isEqualTo("Two");
    assertThat(form.getAllText(messages)).containsExactly("One", "Two");
    assertThat(form.getAllText(
        messages,
        options -> options.setHasNotText("One")))
        .containsExactly("Two");
    assertThat(form.getElementCount(
        choiceRows,
        options -> options.setHasText("Preferred")))
        .isEqualTo(1);
    assertThat(form.isPresent(
        choiceRows,
        options -> options.setHasText("Preferred")))
        .isTrue();

    form
        .uncheck(choiceInput, options -> options.setHasText("Preferred"))
        .waitUntilHidden(
            choiceRows,
            options -> options.setHasText("Hidden option"));

    assertThat(form.isChecked(
        choiceInput,
        options -> options.setHasText("Preferred")))
        .isFalse();
  }

  private static final class AccountForm extends Base {
    private static final LocatorRef READY = LocatorRefs.selector("h1");
    private static final LocatorRef ROWS = LocatorRefs.selector(".field-row");
    private static final LocatorRef ACTIVE_ROW = LocatorRefs.selector(
        ".field-row",
        new Page.LocatorOptions().setHasText("Secondary"));

    private final LocatorRef initializedAfterSuper = READY;

    private AccountForm(Page page) {
      super(page);
    }

    private AccountForm open() {
      actions().setContent("""
          <h1>Account</h1>
          <label>First name <input></label>
          <label>Last name <input></label>
          <div class="field-row">Primary <input></div>
          <div class="field-row">Secondary <input></div>
          <div class="field-row">Secondary Archived <input></div>
          """);
      waitUntilLoaded();
      return this;
    }

    private AccountForm openWithControls() {
      actions().setContent("""
          <h1>Account controls</h1>
          <input id="command" onkeydown="
            if (event.key === 'Enter') this.dataset.submitted = this.value;
          ">
          <div class="choice-row" data-kind="preferred">
            Preferred <input type="checkbox">
          </div>
          <div class="choice-row">
            Disabled <input type="checkbox" disabled>
          </div>
          <div class="choice-row" hidden>
            Hidden option <input type="checkbox">
          </div>
          <button id="double-button" ondblclick="
            document.querySelector('#status').hidden = false;
          ">Double</button>
          <p id="status" hidden>Ready</p>
          <p class="message">One</p>
          <p class="message">Two</p>
          """);
      waitUntilLoaded();
      return this;
    }

    @Override
    protected LocatorRef loadableSelector() {
      return initializedAfterSuper;
    }
  }
}
