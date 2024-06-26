/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NotSyncedPropertyUpdateIT extends ChromeBrowserTest {

    @Test
    public void unsyncedPropertyUpdate_internalError() {
        open();

        WebElement input = findElement(By.tagName("input"));
        input.sendKeys("bar", Keys.ENTER);

        // system error which means there is an exception on the server side
        Assert.assertTrue(isElementPresent(By.className("v-system-error")));
    }

    @Override
    protected String getTestPath() {
        return "/sync-rpc/unsync-property";
    }
}
