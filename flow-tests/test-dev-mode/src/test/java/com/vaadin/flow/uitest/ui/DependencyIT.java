/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.io.IOException;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.DevToolsElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DependencyIT extends ChromeBrowserTest {

    private static final String RED = "rgba(255, 0, 0, 1)";
    private static final String BLUE = "rgba(0, 0, 255, 1)";

    @Test
    public void styleInjection() {
        open();
        // Initial stylesheet makes all text red
        Assert.assertEquals(RED, findElementById("hello").getCssValue("color"));

        // Inject stylesheet which makes text blue
        findElementById("loadBlue").click();
        Assert.assertEquals(BLUE,
                findElementById("hello").getCssValue("color"));
    }

    @Test
    public void scriptInjection() {
        open();
        // Initial JS registers a body click handler
        clickElementWithJs(findElement(By.tagName("body")));
        String addedBodyText = findElement(By.cssSelector(".body-click-added"))
                .getText();
        Assert.assertEquals(
                "Click on body, reported by JavaScript click handler",
                addedBodyText);

        // Inject scripts
        findElementById("loadJs").click();
        waitForElementPresent(By.id("read-global-var-text"));
        String addedJsText = findElementById("read-global-var-text").getText();
        Assert.assertEquals(
                "Second script loaded. Global variable (window.globalVar) is: 'Set by set-global-var.js'",
                addedJsText);
    }

    protected WebElement findElementById(String id) {
        return findElement(By.id(id));
    }
}
