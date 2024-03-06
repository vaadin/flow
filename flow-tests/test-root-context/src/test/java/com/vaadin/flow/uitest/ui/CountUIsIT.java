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
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.BrowserUtil;

public class CountUIsIT extends ChromeBrowserTest {

    @Test
    @Ignore("Ignored because the test is flaky: https://github.com/vaadin/flow/issues/10493")
    public void countUisNumer_onlyOneUIShouldBeInitiialized() {
        if (!BrowserUtil.isChrome(getDesiredCapabilities())) {
            // limit this test for being executed in one browser only
            return;
        }
        open();

        $(NativeButtonElement.class).first().click();

        WebElement uisCount = findElement(By.id("uis"));
        int count = Integer.parseInt(uisCount.getText());

        // there should not be any UI instance which is created but never has
        // been navigated (no any enter event into a navigation target)
        Assert.assertEquals(0, count);
    }
}
