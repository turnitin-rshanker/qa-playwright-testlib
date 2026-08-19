package com.turnitin.playwright.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.turnitin.playwright.Base;
import com.turnitin.playwright.actions.TextFieldInput;
import com.turnitin.playwright.locators.LocatorRef;
import com.turnitin.playwright.locators.LocatorRefs;

import java.util.List;

public class InputTextPage extends Base {

    private static final String APP = """
        <label>First name <input id="first-name"></label>
        <label>Last name <input id="last-name"></label>
        <input id="home-phone" class="phone" aria-label="Phone">
        <input id="work-phone" class="phone" aria-label="Phone">
        <div class="field-row"><span>Postal code</span><input id="postal-code"></div>
        <script>
          window.fillOrder = [];
          document.querySelectorAll('input').forEach(input =>
            input.addEventListener('input', () => window.fillOrder.push(input.id)));
        </script>
      """;

    private static final LocatorRef FIRST_NAME = LocatorRefs.label(
            "First name",
            new Page.GetByLabelOptions().setExact(true));
    private static final LocatorRef LAST_NAME = LocatorRefs.role(
            AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Last name").setExact(true));
    private static final LocatorRef WORK_PHONE = LocatorRefs.selector("input.phone").nth(1);
    private static final LocatorRef POSTAL_CODE = LocatorRefs.selector(
            ".field-row",
            new Page.LocatorOptions().setHasText("Postal code")).child("input");

    private static final TextFieldInput SENSITIVE_INPUT = TextFieldInput.of(
            LocatorRefs.selector("input["),
            "must-not-leak");

    public InputTextPage(Page page) {
        super(page);
    }

    public InputTextPage open() {
        actions().setContent(APP);
        waitUntilLoaded();
        return this;
    }

    public void setValues() {
        setValues(List.of(
                TextFieldInput.of(FIRST_NAME, "Ada"),
                TextFieldInput.of(LAST_NAME, "Lovelace"),
                TextFieldInput.of(WORK_PHONE, "+1 555 0100"),
                TextFieldInput.of(POSTAL_CODE, "94107")));
    }

    public String getFirstName() {
        return inputValue(FIRST_NAME);
    }

    public String getLastName() {
        return inputValue(LAST_NAME);
    }

    public String getWorkPhone() {
        return inputValue(WORK_PHONE);
    }

    public String getPostalCode() {
        return inputValue(POSTAL_CODE);
    }

    public void setSensitiveData() {
        setValues(SENSITIVE_INPUT);
    }

    @Override
    protected LocatorRef loadableSelector() {
        return FIRST_NAME;
    }
}
