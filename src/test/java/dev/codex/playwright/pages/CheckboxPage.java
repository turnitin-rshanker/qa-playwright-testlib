package dev.codex.playwright.pages;

import com.microsoft.playwright.Page;
import dev.codex.playwright.Base;
import dev.codex.playwright.locators.LocatorRef;
import dev.codex.playwright.locators.LocatorRefs;

/** Sample page showing that page objects use LocatorRef-based component actions. */
public final class CheckboxPage extends Base {
  private static final String APP = """
      <title>Wrapper sample</title>
      <label>Name <input id="name"></label>
      <label><input id="updates" type="checkbox"> Receive updates</label>
      <label>Priority
        <select id="priority">
          <option value="normal">Normal</option>
          <option value="high">High</option>
        </select>
      </label>
      <button id="save" onclick="
        const result = document.querySelector('#result');
        result.hidden = false;
        result.dataset.state = 'saved';
        result.textContent = document.querySelector('#name').value;
      ">Save</button>
      <p id="result" hidden></p>
      """;

  private static final LocatorRef NAME = LocatorRefs.selector("#name");
  private static final LocatorRef UPDATES = LocatorRefs.selector("#updates");
  private static final LocatorRef PRIORITY = LocatorRefs.selector("#priority");
  private static final LocatorRef SAVE = LocatorRefs.selector("#save");
  private static final LocatorRef RESULT = LocatorRefs.selector("#result");

  public CheckboxPage(Page page) {
    super(page);
  }

  public CheckboxPage open() {
    actions().setContent(APP);
    waitUntilLoaded();
    return this;
  }

  public CheckboxPage enterNameAndEnableUpdates(String value) {
    focus(NAME)
        .setValue(NAME, value)
        .press(NAME, "End")
        .check(UPDATES)
        .hover(SAVE);
    return this;
  }

  public CheckboxPage selectPriority(String label) {
    selectByLabel(PRIORITY, label);
    return this;
  }

  public CheckboxPage save() {
    click(SAVE).waitUntilVisible(RESULT);
    return this;
  }

  public String selectedPriority() {
    return inputValue(PRIORITY);
  }

  public String enteredName() {
    return inputValue(NAME);
  }

  public boolean updatesEnabled() {
    return isChecked(UPDATES);
  }

  public String resultText() {
    return getText(RESULT);
  }

  public String resultState() {
    return getAttribute(RESULT, "data-state");
  }

  @Override
  protected LocatorRef loadableSelector() {
    return NAME;
  }
}
