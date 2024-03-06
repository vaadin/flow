/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ExceptionStacktraceIT extends ChromeBrowserTest {

    @Test
    public void loggerAbsenceWarningAndStacktrace() {
        open();

        WebElement main = findElements(By.cssSelector("body > div")).stream()
                .filter(element -> element.getAttribute("class").isEmpty())
                .findFirst().get();

        Assert.assertFalse(
                "There should be no warning about SLF4J absence because the test project should have slf4j bindings",
                findElements(By.cssSelector("body > div > div")).stream()
                        .anyMatch(div -> div.getText().toUpperCase()
                                .contains("SLF4J")));

        WebElement stacktrace = main.findElement(By.tagName("pre"));

        // The first string is the op level exception thrown in the core, the
        // second string is the cause of the exception. Both should be in the
        // stacktrace
        Assert.assertThat("There is no stacktrace on the page",
                stacktrace.getText(),
                CoreMatchers.allOf(
                        CoreMatchers.containsString(
                                IllegalArgumentException.class.getName()),
                        CoreMatchers.containsString(
                                "java.lang.RuntimeException: Error here!")));
    }

}
