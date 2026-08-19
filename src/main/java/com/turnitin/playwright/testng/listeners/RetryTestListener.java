package com.turnitin.playwright.testng.listeners;

import com.turnitin.playwright.testng.RetryTestAnalyzer;
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
