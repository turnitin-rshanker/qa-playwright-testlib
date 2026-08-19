package dev.codex.playwright.core;

import com.microsoft.playwright.Page;
import io.qameta.allure.Attachment;
import java.util.Objects;

/** Small attachment helpers for consumers that need an explicit Allure step. */
public final class AllureHelper {
  private AllureHelper() {
  }

  @Attachment(value = "{0}", type = "image/png")
  public static byte[] attachScreenshot(String name, Page page) {
    Objects.requireNonNull(name, "name");
    return Objects.requireNonNull(page, "page").screenshot(
        new Page.ScreenshotOptions().setFullPage(true));
  }

  @Attachment(value = "{0}", type = "text/plain")
  public static String attachMessage(String name, String message) {
    Objects.requireNonNull(name, "name");
    return Objects.requireNonNull(message, "message");
  }

  /** @deprecated Use {@link #attachMessage(String, String)}. */
  @Deprecated(forRemoval = false)
  @Attachment(value = "Message", type = "text/plain")
  public static String attachMassage(String message) {
    return Objects.requireNonNull(message, "message");
  }
}
