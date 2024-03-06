/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.frontend;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class BrowserLoggingIT extends ChromeBrowserTest {

    @Test
    @Ignore
    public void productionModeHasNoLogEntries() {
        openProduction();
        waitForElementPresent(By.id("elementId"));

        assertThat(
                "Flow in production mode should output nothing into the console",
                getLogEntriesCount(), is(0L));
    }

    @Test
    public void nonProductionModeHasLogEntries() {
        open();
        waitForElementPresent(By.id("elementId"));

        assertThat(
                "Flow in production mode should output nothing into the console",
                getLogEntriesCount(), greaterThan(0L));
    }

    private Long getLogEntriesCount() {
        // see java script imported in corresponding test view
        return (Long) executeScript("return window.allLogMessages.length;");
    }
}
