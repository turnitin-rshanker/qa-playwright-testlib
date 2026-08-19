# Java Playwright Test Framework

A production-minded Playwright starter using Java 21, Maven, TestNG, AssertJ, and Allure. The included tests run against a local in-memory page, so the smoke suite does not depend on an external website.

## What is included

- One isolated Playwright browser context per test
- Parallel TestNG execution with thread-confined sessions
- Chromium, Firefox, and WebKit selection
- Reusable `Component` and explicit-load `Base` classes
- `Component` wrappers for common `LocatorRef` operations
- `PlaywrightActions` for page-level operations and managed routing
- Per-call native locator options and ordered edit-field input
- Environment-aware YAML configuration with strict typed validation
- Failure screenshots and retain-on-failure Playwright traces
- Dedicated, bounded, secret-redacted browser console and network-call reports
- Automatic Allure attachments
- Optional TestNG retries and automatic Allure environment metadata
- Exception-safe, idempotent browser cleanup

## Prerequisites

- Java 21+
- Maven 3.9+

Install the browser once:

```shell
mvn exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium"
  ```

Run the suite:

```shell
mvn clean test "-Dtest.env=local"
```

Run headed or select another installed browser:

```shell
mvn test "-Dtest.env=local" "-Dheadless=false" "-Dbrowser=chromium"
mvn test "-Dtest.env=local" "-Dbrowser=chrome"
mvn test "-Dtest.env=local" "-Dbrowser=msedge"
mvn test "-Dtest.env=local" "-Dbrowser=firefox"
mvn test "-Dtest.env=local" "-Dbrowser=webkit"
```

Select an application environment independently of the browser:

```powershell
mvn test "-Dtest.env=local" "-Denv=qa"
mvn test "-Dtest.env=local" "-Denv=staging" "-Dbrowser=firefox" "-Dheadless=false"
```

`chrome` and `msedge` use the locally installed branded browser through Playwright's Chromium channel support. `chromium`, `firefox`, and `webkit` use Playwright-managed browser binaries.

Run the same tests against Chromium, Firefox, and WebKit without TestNG XML:

```powershell
foreach ($browser in 'chromium', 'firefox', 'webkit') {
  mvn test "-Dtest.env=local" "-Dbrowser=$browser"
}
```

Install all three bundled browser engines before the first matrix run:

```shell
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium firefox webkit"
```

Maven Surefire discovers `*Test.java` classes directly and runs their methods in parallel. No `testng.xml` file is required. Each command selects one browser through framework configuration, and artifact directories include that browser name.

Configuration precedence is YAML defaults, the selected YAML environment, `PW_*` environment variables, then Java `-D` properties. The environment itself is selected by `-Denv`, then `PW_ENV`, then `defaultEnvironment` from YAML.

## Project layout

```text
src/main/java/dev/codex/playwright/
  Base.java      explicit, post-construction loaded-state contract
  Component.java reusable locator/action facade
  actions/       page operations, managed routing, and form input records
  config/        typed configuration and browser/artifact policies
  core/          PlaywrightSession, lifecycle, diagnostics, artifacts
  locators/      lazy, option-aware locator definitions
  testng/        session context, Allure metadata, retries, and listeners
src/test/java/dev/codex/playwright/
  pages/         sample TodoPage
  tests/         BaseTest fixture and sample TestNG tests
src/test/resources/
  env-local.yaml
```

## Add a real page and test

Create a page object extending `Base`, accept `Page` in its constructor, implement `loadableSelector()`, and prefer `getByRole`, `getByLabel`, or stable test IDs. Tests extend `com.turnitin.playwright.BaseTest` and obtain the current thread-confined page with `getPage()`.

`BaseTest` exposes the protected `getPage()` native escape hatch, a non-owning `actions()` facade, and validated navigation conveniences. It deliberately does not expose the listener-owned `AutoCloseable` session, so tests cannot accidentally close it. Routine operations that return managed registrations go through `actions()`. The service-loaded `PlaywrightTestListener` starts and closes each session, identifies failures, reports the browser parameter, and attaches artifacts directly to the active Allure test. It is applied only to tests extending `BaseTest`; ordinary unit tests do not launch a browser.

Do not close the page returned by `getPage()` in test code. The listener closes its page context, browser, and Playwright instance during teardown.

The session exists only while the `@Test` method is running. Build page objects inside the test method rather than calling `getPage()` from TestNG configuration methods.

`Base` inherits the `Component` facade. It preserves Playwright auto-waiting and supports fluent `LocatorRef` action chains:

```java
public LoginPage signIn(String username, String password) {
  setValue(usernameInput, username)
      .setValue(passwordInput, password)
      .click(signInButton)
      .waitUntilVisible(accountMenu);
  return this;
}
```

`Component` includes click/double-click, set/clear value, set-value-and-enter, key presses, check/uncheck, hover/focus, select options, file upload, drag-and-drop, scrolling, attached/detached/visible/hidden waits, ordered form input, and common text/state reads. These methods accept `LocatorRef`; applicable methods also accept a per-call `Consumer<Page.LocatorOptions>`.

```java
selectByLabel(prioritySelect, "High");
upload(fileInput, Path.of("fixtures", "avatar.png"));
dragTo(sourceCard, destinationColumn);
waitForAttached(successMessage);
```

Use `Component` methods for locator interactions. Use `actions()` for page-level operations such as managed routing, page history, load-state waits, or test-only `setContent`. Use `getPage()` only when a native Playwright API is not covered by either facade.

Use the managed route wrapper instead of calling `getPage().route(...)` directly:

```java
actions().route("https://app.test/**", route ->
    route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody("{\"ok\":true}")));
```

Playwright 1.62 returns an `AutoCloseable` registration from `Page.route(...)`. The wrapper retains that registration for the test and the listener closes it in reverse registration order during exception-safe teardown. String, regular-expression, predicate, and native `Page.RouteOptions` overloads are supported.

### Safe page navigation

Use `navigateToPage(...)` for a relative page URL. Playwright resolves it against the selected environment's `baseUrl`:

```java
navigateToPage("/login?returnTo=%2Faccount");
```

Use `openUrl(...)` when the caller supplies a complete external URL:

```java
openUrl("https://docs.example.test/help#sign-in");
```

`navigateToPage(...)` rejects absolute and protocol-relative URLs. `openUrl(...)` accepts only complete HTTP or HTTPS URLs with a valid host and port. Both reject blank or malformed values before Playwright sends a request. The backward-compatible `navigate(...)` method accepts either a validated relative page URL or a complete HTTP(S) URL.

Before relative navigation, the framework logs the configured base URL, supplied path, and resolved destination:

```text
Navigating: baseUrl=https://app.test/, path=/account, resolvedUrl=https://app.test/account
```

This value is calculated for visibility only. Playwright still receives `/account` and performs the actual `BrowserContext.baseURL` resolution. Query and fragment contents are replaced with `<redacted>` in the log.

Common locator operations execute directly inside `Component`; there is no intermediate `LocatorActions` or `FormActions` layer. This preserves Playwright's native auto-waiting and exception behavior while keeping the page-object API fluent.

For a page that has a formal ready state, extend `Base`, implement `loadableSelector()`, and call `waitUntilLoaded()` only after the subclass locator fields have been initialized—for example, from `open()`. The base constructor never invokes an overridable method.

### Option-aware locators and form lists

Use `LocatorRef` when a locator needs native Playwright options or must be reused against the current browser page:

```java
LocatorRef email = LocatorRefs.role(
    AriaRole.TEXTBOX,
    new Page.GetByRoleOptions()
        .setName("Work email")
        .setExact(true));

LocatorRef secondPhone = LocatorRefs.selector("input.phone").nth(1);
```

`LocatorRefs` supports selectors, roles, labels, placeholders, text, test IDs, `Page.LocatorOptions`, `Locator.FilterOptions`, first/last/`nth`, and child composition.

`Component` also accepts additional selector options at the point of use. These options resolve against the current page, flow through `first`, `last`, `nth`, and child composition, and merge with options already stored by `LocatorRefs.selector(...)`:

```java
setValue(
    LocatorRefs.selector(".field-row").child("input"),
    options -> options.setHasText("Work email"),
    "ada@example.test");
```

Use the typed option objects in `LocatorRefs.role(...)`, `label(...)`, `placeholder(...)`, or `text(...)` for those locator families.

Fill an ordered collection of edit fields with `TextFieldInput`:

```java
setValues(List.of(
    TextFieldInput.of(firstName, "Ada"),
    TextFieldInput.of(lastName, "Lovelace"),
    TextFieldInput.of(email, "ada@example.test"),
    TextFieldInput.of(secondPhone, "+1 555 0100")));
```

Selector, role, label, placeholder, text, and test-ID definitions resolve lazily against the current test page, so they remain safe across parallel browser sessions. `fixed(...)` references and native options containing page-bound `has`/`hasNot` locators should only be used with the page that created them. Filling stops at the first failure and throws `BulkInputException` with the zero-based list index and locator description. Field values are excluded from both its message and `TextFieldInput.toString()`.

## Environment configuration

Define shared values and environment overrides in `src/test/resources/env-local.yaml`:

```yaml
defaultEnvironment: local

defaults:
  browser: chromium
  headless: true
  baseUrl: https://app.test/
  actionTimeoutMs: 15000
  navigationTimeoutMs: 30000
  viewport:
    width: 1440
    height: 900
  traceMode: retain-on-failure
  screenshotMode: only-on-failure
  diagnosticsEnabled: true
  artifactsDirectory: target/artifacts

environments:
  local: {}
  qa:
    baseUrl: https://qa.example.test
  staging:
    baseUrl: https://staging.example.test
```

The loader reads this resource through the classpath, so it also works when framework classes come from a packaged JAR. Unknown keys, unknown environments, duplicate YAML keys, invalid types, and invalid values fail immediately with sanitized errors.

Keep credentials and tokens outside YAML. Supply them through your CI secret store or environment variables used directly by test code; the framework configuration schema intentionally does not accept credential fields.

Select the resource file with `-Dtest.env=local` or `TEST_ENV=local`, then use `-Denv` or `PW_ENV` to select a profile inside that file. For example:

```powershell
mvn test "-Dtest.env=local" "-Denv=qa"
```

Example PowerShell environment selection:

```powershell
$env:PW_ENV = "qa"
$env:PW_BROWSER = "firefox"
$env:TEST_ENV = "local"
mvn test
```

## Artifacts and reports

- Browser artifacts: `target/artifacts/<test>/<run>/`
- Allure results: `target/allure-results/`
- TestNG/Maven results: `target/surefire-reports/`

Every test attaches `browser-console.log`, `network-calls.jsonl`, and `browser-diagnostics.log` to Allure. The network report records request, response, and failure metadata without collecting bodies or headers. For a failed test, the run directory also contains `screenshot.png` and `trace.zip`. Open a trace with:

```shell
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace target/artifacts/path/to/trace.zip"
```

If the Allure CLI is installed, render the report with:

```shell
allure serve target/allure-results
```

The service-loaded `AllureEnvWriter` records the selected environment and configured browser in `environment.properties`.

The lifecycle does not use `IHookable`, `@BeforeMethod`, `@AfterMethod`, `@Listeners`, or TestNG XML. TestNG discovers the listener from `META-INF/services/org.testng.ITestNGListener`.

Retries are disabled by default. Enable one retry with `mvn test "-Dretries=1"`. Retry parsing and reporting are non-destructive: a retry does not suppress the original Playwright cleanup or evidence capture.

## Useful configuration

| YAML key | Java property | Environment variable | Default |
|---|---|---|---:|
| `browser` | `browser` | `PW_BROWSER` | `chromium` |
| `headless` | `headless` | `PW_HEADLESS` | `true` |
| `baseUrl` | `base.url` | `PW_BASE_URL` | `about:blank` |
| `actionTimeoutMs` | `action.timeout.ms` | `PW_ACTION_TIMEOUT_MS` | `15000` |
| `navigationTimeoutMs` | `navigation.timeout.ms` | `PW_NAVIGATION_TIMEOUT_MS` | `30000` |
| `viewport.width` | `viewport.width` | `PW_VIEWPORT_WIDTH` | `1440` |
| `viewport.height` | `viewport.height` | `PW_VIEWPORT_HEIGHT` | `900` |
| `slowMoMs` | `slowmo.ms` | `PW_SLOWMO_MS` | `0` |
| `ignoreHttpsErrors` | `ignore.https.errors` | `PW_IGNORE_HTTPS_ERRORS` | `false` |
| `traceMode` | `trace.mode` | `PW_TRACE_MODE` | `retain-on-failure` |
| `screenshotMode` | `screenshot.mode` | `PW_SCREENSHOT_MODE` | `only-on-failure` |
| `diagnosticsEnabled` | `diagnostics.enabled` | `PW_DIAGNOSTICS_ENABLED` | `true` |
| `artifactsDirectory` | `artifacts.dir` | `PW_ARTIFACTS_DIR` | `target/artifacts` |

Avoid placing secrets in test names, URLs, or browser console messages because those values may appear in diagnostic artifacts.
