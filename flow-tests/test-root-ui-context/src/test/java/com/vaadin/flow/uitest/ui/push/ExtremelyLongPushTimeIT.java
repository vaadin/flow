/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.ExtremelySlowTest;
import com.vaadin.testbench.parallel.ExcludeFromSuite;

@ExcludeFromSuite
@Category(ExtremelySlowTest.class)
@Ignore
public class ExtremelyLongPushTimeIT extends AbstractLogTest {

    private static final int ONE_HOUR_IN_MS = 60 * 60 * 1000;

    @Test
    public void test24HourPush() throws Exception {
        open();

        // Without this there is a large chance that we will wait for all pushes
        // to complete before moving on
        getCommandExecutor().disableWaitForVaadin();

        // Wait for startButton to be present
        waitForElementVisible(By.id("startButton"));

        // Start the test
        findElement(By.id("startButton")).click();

        // Wait for push to start. Should take 60s
        waitUntil(textToBePresentInElement(() -> getLastLog(), "Package "),
                120);

        // Check every hour that push is still going on
        for (int i = 0; i < 24; i++) {
            Thread.sleep(ONE_HOUR_IN_MS);
            ensureStillPushing();
        }

    }

    private void ensureStillPushing() {
        String logValue = getLastLog().getText();
        // Wait for the log value to change. Should take max 60s
        waitUntilNot(textToBePresentInElement(() -> getLastLog(), logValue),
                120);
    }
}
