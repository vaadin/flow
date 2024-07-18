/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

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

    @Test
    public void loadingUnavailableResources() {
        open();
        findElement(By.id("loadUnavailableResources")).click();

        List<String> errors = findElements(By.className("v-system-error"))
                .stream().map(WebElement::getText).collect(Collectors.toList());
        Assert.assertEquals(2, errors.size());
        // The order for these can be random
        assertTrue("Couldn't find error for not-found.css",
                errors.stream()
                        .filter(s -> s.startsWith("Error loading http://")
                                && s.endsWith("/not-found.css"))
                        .findFirst().isPresent());
        assertTrue("Couldn't find error for not-found.js",
                errors.stream()
                        .filter(s -> s.startsWith("Error loading http://")
                                && s.endsWith("/not-found.js"))
                        .findFirst().isPresent());
    }

    @Test
    @Ignore
    public void loadingUnavailableResourcesProduction() {
        openProduction();
        findElement(By.id("loadUnavailableResources")).click();

        List<WebElement> errors = findElements(By.className("v-system-error"));
        // Should not be shown in production
        Assert.assertEquals(0, errors.size());
    }

    private List<String> getMessages() {
        List<WebElement> elements = findElements(
                By.xpath("html/body/*[@class='message']"));
        List<String> messages = new ArrayList<>();
        for (WebElement element : elements) {
            messages.add(element.getText());
        }
        return messages;
    }

    protected WebElement findElementById(String id) {
        return findElement(By.id(id));
    }
}
