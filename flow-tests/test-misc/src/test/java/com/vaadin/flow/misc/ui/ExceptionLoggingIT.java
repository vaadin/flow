/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExceptionLoggingIT extends ChromeBrowserTest {

    @Test
    public void productionModeExceptionViaServerJsShouldLogException() {
        open();
        findElement(By.id("exception")).click();

        // Checking that the log count is exactly 1 also ensures that no info or
        // debug level messages were logged before the exception message.
        assertThat("Flow in production mode should output exception to console",
                getLogEntriesCount(), is(1L));
        assertThat("Error message should have contained 'foo'",
                checkFirstErrorMessageContains("foo"));
    }

    @Test
    public void productionModeExceptionViaExternalJsShouldLogException() {
        open();
        findElement(By.id("externalException")).click();

        // Checking that the log count is exactly 1 also ensures that no info or
        // debug level messages were logged before the exception message.
        assertThat("Flow in production mode should output exception to console",
                getLogEntriesCount(), is(1L));
        assertThat("Error message should have contained 'bar'",
                checkFirstErrorMessageContains("bar"));
    }

    private Long getLogEntriesCount() {
        return (Long) executeScript("return window.allLogMessages.length;");
    }

    private boolean checkFirstErrorMessageContains(String shouldContain) {
        return ((String) executeScript("return window.allLogMessages[0];"))
                .contains(shouldContain);
    }

    @Override
    protected String getTestPath() {
        return "/exception-logging";
    }
}