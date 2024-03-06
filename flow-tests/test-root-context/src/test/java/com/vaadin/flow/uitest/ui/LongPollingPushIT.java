/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class LongPollingPushIT extends ChromeBrowserTest {

    @Test
    public void openPage_thereAreNoErrorsInTheConsole() {
        open();
        checkLogsForErrors(msg -> msg.contains("sockjs-node")
                || msg.contains("[WDS] Disconnected!"));

        waitForElementNotPresent(By.id("child"));

        WebElement button = findElement(By.id("visibility"));

        button.click();

        waitForElementPresent(By.id("child"));
        WebElement span = findElement(By.id("child"));

        Assert.assertEquals("Some text", span.getAttribute("innerHTML"));
        /*
         * SockJS client may try to connect to sockjs node server:
         * https://github.com/sockjs/sockjs-node/blob/master/README.md.
         *
         * This entry may be ignored.
         */
        checkLogsForErrors(msg -> msg.contains("sockjs-node")
                || msg.contains("[WDS] Disconnected!"));
    }

}
