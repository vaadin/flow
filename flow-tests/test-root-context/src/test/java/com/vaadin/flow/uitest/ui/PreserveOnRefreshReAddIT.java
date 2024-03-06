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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshReAddIT extends ChromeBrowserTest {

    @Test
    public void replaceComponentAfterRefresh_componentIsReplaced() {
        open();

        findElement(By.id("set-text")).click();
        findElement(By.id("set-another-text")).click();

        WebElement container = findElement(By.id("container"));

        // self check
        Assert.assertEquals("Another Text", container.getText());

        open();

        findElement(By.id("set-text")).click();

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
        container = findElement(By.id("container"));
        Assert.assertEquals("Text", container.getText());
    }
}
