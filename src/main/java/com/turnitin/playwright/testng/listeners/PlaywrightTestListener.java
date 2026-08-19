package com.turnitin.playwright.testng.listeners;


import com.turnitin.playwright.BaseTest;
import com.turnitin.playwright.testng.PlaywrightTestContext;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;


/** Starts and closes Playwright around every TestNG test extending BaseTest. */
public final class PlaywrightTestListener implements IInvokedMethodListener {

  @Override
  public void beforeInvocation(IInvokedMethod method, ITestResult result) {
    if (isPlaywrightTest(method, result)) {
      PlaywrightTestContext.start(result);
    }
  }

  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult result) {
    if (isPlaywrightTest(method, result)) {
      PlaywrightTestContext.closeAndAttach(result);
    }
  }

  private static boolean isPlaywrightTest(IInvokedMethod method, ITestResult result) {
    return method.isTestMethod() && result.getInstance() instanceof BaseTest;
  }
}
