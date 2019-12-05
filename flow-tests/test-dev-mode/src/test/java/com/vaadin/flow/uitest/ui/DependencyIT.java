/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
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
    public void htmlInjection() {
        open();

        // Initial HTML import logs a message on the page
        List<String> messages = getMessages();

        Assert.assertEquals(3, messages.size());
        assertTrue(messages.contains("HTML import 4 companion JS loaded"));
        assertTrue(messages.contains("Messagehandler initialized in HTML import 1"));
        assertTrue(messages.contains("Messagehandler initialized in HTML import 4"));

        // Inject html
        findElementById("loadHtml").click();
        messages = getMessages();

        Assert.assertEquals(5, messages.size());
        assertTrue(messages.contains("HTML import 2 loaded"));
        assertTrue(messages.contains("HTML import 3 loaded"));
    }

    @Test
    public void mixedHtmlAndJsInjection() {
        open();
        findElement(By.id("loadMixed")).click();

        List<String> messages = getMessages();

        Assert.assertEquals("script1 is loaded",
                messages.get(messages.size() - 4));
        Assert.assertEquals("Mixed HTML import 1 loaded",
                messages.get(messages.size() - 3));
        Assert.assertEquals("script2 is loaded",
                messages.get(messages.size() - 2));
        Assert.assertEquals("Mixed HTML import 2 loaded",
                messages.get(messages.size() - 1));
    }

    @Test
    public void loadingUnavailableResources() {
        open();
        findElement(By.id("loadUnavailableResources")).click();

        List<String> errors = findElements(By.className("v-system-error"))
                .stream().map(WebElement::getText).collect(Collectors.toList());
        Assert.assertEquals(3, errors.size());
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
        assertTrue("Couldn't find error for not-found.html",
                errors.stream()
                        .filter(s -> s.startsWith("Error loading http://")
                                && s.endsWith("/not-found.html"))
                        .findFirst().isPresent());
    }

    @Test
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
