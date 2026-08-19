package dev.codex.playwright.testng.listeners;

import dev.codex.playwright.testng.RetryTestAnalyzer;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class RetryTestListener implements IAnnotationTransformer {
  @Override
  @SuppressWarnings("rawtypes")
  public void transform(
      ITestAnnotation testAnnotation,
      Class testClass,
      Constructor testConstructor,
      Method testMethod
  ) {
    testAnnotation.setRetryAnalyzer(RetryTestAnalyzer.class);
  }

}
