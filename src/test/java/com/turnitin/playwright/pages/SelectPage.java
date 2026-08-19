package com.turnitin.playwright.pages;

import com.microsoft.playwright.Page;
import com.turnitin.playwright.Base;
import com.turnitin.playwright.locators.LocatorRef;
import com.turnitin.playwright.locators.LocatorRefs;

import java.nio.file.Path;
import java.util.List;

/** Sample page showing that page objects use LocatorRef-based component actions. */
public final class SelectPage extends Base {
  private static final String APP = """
        <div class="select-row">Primary
          <select><option value="normal">Normal</option><option value="high">High</option></select>
        </div>
        <div class="select-row">Secondary
          <select><option value="normal">Normal</option><option value="high">High</option></select>
        </div>
        <input id="upload" type="file">
        <div id="source" draggable="true">Source</div>
        <div id="target">Target</div>
        <div style="height: 1500px"></div>
        <button id="bottom">Bottom</button>
        <script>
          const target = document.querySelector('#target');
          target.addEventListener('dragover', event => event.preventDefault());
          target.addEventListener('drop', event => {
            event.preventDefault();
            target.dataset.dropped = 'true';
          });
        </script>
        """;

  LocatorRef rowSelect = LocatorRefs.selector(".select-row")
          .child("select");

  LocatorRef upload = LocatorRefs.selector("#upload");
  LocatorRef source = LocatorRefs.selector("#source");
  LocatorRef target = LocatorRefs.selector("#target");
  LocatorRef bottom = LocatorRefs.selector("#bottom");
  LocatorRef missing = LocatorRefs.selector("#missing");

  public SelectPage(Page page) {
    super(page);
  }

  public SelectPage open() {
    actions().setContent(APP);
    waitUntilLoaded();
    return this;
  }

  public List<String> getRowHavingText(String rowText) {
    return selectByLabel(
            rowSelect,
            options -> options.setHasText(rowText),
            "High");
  }

  public List<String> getSelectedValueHavingText(String selectText) {
    return selectByValue(
            rowSelect,
            options -> options.setHasText(selectText),
            "normal");
  }

  public void performActions() {
    upload(upload, Path.of("pom.xml"))
            .dragTo(source, target)
            .scrollIntoView(bottom)
            .waitForAttached(bottom)
            .waitForDetached(missing);
  }

  public String getUploadedValue() {
    return inputValue(upload);
  }

  public String getTargetValue() {
    return getAttribute(target, "data-dropped");
  }

  public void performUpload() {
    upload(upload);
  }

  @Override
  protected LocatorRef loadableSelector() {
    return rowSelect.first();
  }
}
