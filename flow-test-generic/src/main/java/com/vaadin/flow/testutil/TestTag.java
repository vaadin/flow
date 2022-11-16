package com.vaadin.flow.testutil;

/**
 * List of JUnit 5 tags that are used to group IT tests.
 */
public interface TestTag {

    // prefix for compatibility with JUnit 4's @Category
    String CHROME_TESTS = "com.vaadin.flow.testcategory.ChromeTests";

    String IGNORE_OSGI = "com.vaadin.flow.testcategory.IgnoreOSGi";

    String PUSH_TESTS = "com.vaadin.flow.testcategory.PushTests";

    String SCREENSHOT_TESTS = "com.vaadin.flow.testcategory.ScreenshotTests";

    String SLOW_TESTS = "com.vaadin.flow.testcategory.SlowTests";

    String EXTREMELY_SLOW_TEST = "com.vaadin.flow.testcategory.ExtremelySlowTest";

}
