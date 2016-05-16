/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class DependencyIT extends PhantomJSTest {

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
        findElement(By.cssSelector("body")).click();
        String addedBodyText = findElement(By.cssSelector(".body-click-added"))
                .getText();
        Assert.assertEquals(
                "Click on body, reported by JavaScript click handler",
                addedBodyText);

        // Inject scripts
        findElementById("loadJs").click();
        String addedJsText = findElementById("read-global-var-text").getText();
        Assert.assertEquals(
                "Second script loaded. Global variable (window.globalVar) is: 'Set by set-global-var.js'",
                addedJsText);
    }

    @Test
    public void htmlInjection() {
        open();
        // Initial HTML import logs a message on the page
        findElement(By.cssSelector("body")).click();

        List<String> messages = getMessages();
        Assert.assertEquals("Messagehandler initialized in HTML import 1",
                messages.get(messages.size() - 1));

        // Inject html
        findElementById("loadHtml").click();
        messages = getMessages();

        Assert.assertEquals("HTML import 2 loaded",
                messages.get(messages.size() - 2));
        Assert.assertEquals("HTML import 3 loaded",
                messages.get(messages.size() - 1));
    }

    private List<String> getMessages() {
        List<WebElement> elements = findElements(By.className("message"));
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
