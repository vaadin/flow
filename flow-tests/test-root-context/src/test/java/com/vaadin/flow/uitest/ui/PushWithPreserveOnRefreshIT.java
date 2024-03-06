/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreOSGi.class)
public class PushWithPreserveOnRefreshIT extends ChromeBrowserTest {

    @Test
    public void ensurePushWorksAfterRefresh() {
        open();

        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));

        waitUntil(driver -> !loadingIndicator.isDisplayed());

        TestBenchElement button = $(TestBenchElement.class).id("click");
        button.click();
        button.click();
        Assert.assertEquals("Button has been clicked 2 times", getLastLog());

        open();
        Assert.assertEquals("Button has been clicked 2 times", getLastLog());
        button = $(TestBenchElement.class).id("click");
        button.click();
        Assert.assertEquals("Button has been clicked 3 times", getLastLog());
    }

    private String getLastLog() {
        List<WebElement> logs = findElements(By.className("log"));
        if (logs.isEmpty()) {
            return null;
        }
        return logs.get(logs.size() - 1).getText();
    }
}
