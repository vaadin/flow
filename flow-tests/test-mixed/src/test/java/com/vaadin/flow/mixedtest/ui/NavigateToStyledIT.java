/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NavigateToStyledIT extends ChromeBrowserTest {

    @Test
    public void navigateToStyledComponent() {
        open();

        findElement(By.id("navigate")).click();

        waitForElementPresent(By.id("styling-div"));

        WebElement div = findElement(By.id("styling-div"));

        String color = div.getCssValue("color");
        // The red color
        Assert.assertEquals("rgba(255, 0, 0, 1)", color);
    }

    @Override
    protected String getTestPath() {
        return "/context-path/servlet-path/navigate-to-styled";
    }
}
