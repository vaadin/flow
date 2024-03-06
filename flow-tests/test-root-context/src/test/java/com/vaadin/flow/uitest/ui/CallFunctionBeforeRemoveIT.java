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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CallFunctionBeforeRemoveIT extends ChromeBrowserTest {

    @Test
    public void noErrorOnJSExecutionBeforeDetach() {
        open();

        findElement(By.tagName("button")).click();

        waitUntil(driver -> !isElementPresent(By.className("v-system-error")));
        Assert.assertFalse(isElementPresent(By.className("v-system-error")));
    }
}
