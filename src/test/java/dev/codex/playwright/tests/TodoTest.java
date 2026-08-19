package dev.codex.playwright.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Route;
import dev.codex.playwright.pages.TodoPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

@Epic("Sample application")
@Feature("Todo list")
public final class TodoTest extends UnitBaseTest {

  @Test
  @Description("A user can add tasks and complete one of them")
  public void addsAndCompletesTasks() {
    TodoPage todos = new TodoPage(getPage())
        .open()
        .addTask("Build the framework")
        .addTask("Run the tests")
        .completeTask("Build the framework");

    assertThat(todos.taskNames()).containsExactly("Build the framework", "Run the tests");
    assertThat(todos.taskCount()).isEqualTo(2);
    assertThat(todos.completedTaskCount()).isEqualTo(1);
  }

  @Test
  @Description("A user can delete an existing task")
  public void deletesATask() {
    TodoPage todos = new TodoPage(getPage())
        .open()
        .addTask("Temporary task")
        .deleteTask("Temporary task");

    assertThat(todos.taskNames()).isEmpty();
    assertThat(todos.taskCount()).isZero();
  }

  @Test
  @Description("Browser console messages and all network calls are retained as Allure attachments")
  public void capturesConsoleAndNetworkEvidence() {
    actions().route("https://app.test/**", route -> {
      if (route.request().url().contains("/api/")) {
        route.fulfill(new Route.FulfillOptions()
            .setStatus(201)
            .setContentType("application/json")
            .setBody("{\"created\":true}"));
      } else {
        route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("text/html")
            .setBody("<title>Diagnostics sample</title><h1>Diagnostics sample</h1>"));
      }
    });

    navigate("https://app.test/");
    String status = (String) getPage().evaluate(
        "async () => String((await fetch('/api/tasks?access_token=allure-secret')).status)");
    getPage().evaluate("console.warn('network capture finished password=allure-secret')");

    assertThat(status).isEqualTo("201");
  }

  @Test
  @Description("BaseTest navigation delegates through the listener-owned action facade")
  public void navigatesWithoutTakingOwnershipOfThePage() {
    actions().route("https://app.test/**", route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("text/html")
        .setBody("<title>BaseTest navigation</title><h1>Account</h1>")));
    actions().route("https://external.test/**", route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("text/html")
        .setBody("<title>External navigation</title><h1>Help</h1>")));

    navigate("/account");
    assertThat(getPage().url()).isEqualTo("https://app.test/account");

    openUrl("https://external.test/help");
    assertThat(getPage().url()).isEqualTo("https://external.test/help");
  }
}
