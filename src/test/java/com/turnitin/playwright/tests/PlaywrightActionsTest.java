package com.turnitin.playwright.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;

import com.turnitin.playwright.actions.PlaywrightActions;

import com.turnitin.playwright.pages.CheckboxPage;
import java.util.ArrayList;
import java.util.List;

import com.turnitin.playwright.pages.InputTextPage;
import com.turnitin.playwright.pages.SelectPage;
import org.testng.annotations.Test;

public final class PlaywrightActionsTest extends UnitBaseTest {

  @Test
  public void transfersRouteRegistrationsToTheLifecycleOwner() throws Exception {
    List<AutoCloseable> registrations = new ArrayList<>();
    PlaywrightActions managedActions = new PlaywrightActions(getPage(), registrations::add);

    managedActions.route(
        "https://owned-route.test/**",
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setBody("owned")),
        new Page.RouteOptions().setTimes(1));

    assertThat(registrations).hasSize(1);
    registrations.removeFirst().close();
  }

  @Test
  public void keepsLocatorActionsInComponentUsingLocatorRefs() {
    CheckboxPage page = new CheckboxPage(getPage())
        .open()
        .enterNameAndEnableUpdates("Framework wrappers")
        .selectPriority("High")
        .save();

    assertThat(page.selectedPriority()).isEqualTo("high");
    assertThat(page.enteredName()).isEqualTo("Framework wrappers");
    assertThat(page.updatesEnabled()).isTrue();
    assertThat(page.resultText()).isEqualTo("Framework wrappers");
    assertThat(page.resultState()).isEqualTo("saved");
  }

  @Test
  public void supportsSpecializedLocatorRefOperationsAndPerCallOptions() {
    SelectPage sp = new SelectPage(getPage()).open();

    assertThat(sp.getRowHavingText("Secondary"))
        .containsExactly("high");

    assertThat(sp.getSelectedValueHavingText("Primary"))
        .containsExactly("normal");

    sp.performActions();

    assertThat(sp.getUploadedValue()).endsWith("pom.xml");
    assertThat(sp.getTargetValue()).isEqualTo("true");
    assertThatThrownBy(sp::performUpload)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("at least one upload path is required");
  }

  @Test
  public void fillsAnOrderedListUsingOptionAwareLocatorDefinitions() {
    InputTextPage sp = new InputTextPage(getPage()).open();

    sp.setValues();

    assertThat(sp.getFirstName()).isEqualTo("Ada");
    assertThat(sp.getLastName()).isEqualTo("Lovelace");
    assertThat(sp.getWorkPhone()).isEqualTo("+1 555 0100");
    assertThat(sp.getPostalCode()).isEqualTo("94107");
    assertThat(getPage().evaluate("window.fillOrder"))
        .isEqualTo(List.of("first-name", "last-name", "work-phone", "postal-code"));


    assertThatThrownBy(sp::setSensitiveData)
        .isInstanceOf(com.turnitin.playwright.actions.BulkInputException.class)
        .hasMessageContaining("field 0", "selector=input[")
        .satisfies(exception -> assertThat(exception.getMessage())
            .doesNotContain("must-not-leak"));
  }

  @Test
  public void validatesRelativeAndAbsoluteNavigationBeforeOpeningPages() {
    actions().route("https://app.test/**", route -> route.fulfill(
        new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("text/html")
            .setBody("<title>Base URL page</title><h1>Account</h1>")));
    actions().route("https://external.test/**", route -> route.fulfill(
        new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("text/html")
            .setBody("<title>External page</title><h1>External</h1>")));

    navigate("/accounts?view=active#team");
    assertThat(getPage().url()).isEqualTo("https://app.test/accounts?view=active#team");
    assertThat(actions().title()).isEqualTo("Base URL page");

    openUrl("https://external.test/help?source=framework#navigation");
    assertThat(getPage().url())
        .isEqualTo("https://external.test/help?source=framework#navigation");

    assertThatThrownBy(() -> navigateToPage("https://external.test/account"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pageUrl must be relative to baseUrl");
    assertThatThrownBy(() -> navigateToPage("/account%2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pageUrl is malformed");
    assertThatThrownBy(() -> openUrl("/relative-only"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("url must be an absolute HTTP or HTTPS URL");
  }
}
