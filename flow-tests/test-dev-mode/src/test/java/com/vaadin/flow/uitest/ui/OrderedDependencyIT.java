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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class OrderedDependencyIT extends ChromeBrowserTest {

    private static final String RED = "rgba(255, 0, 0, 1)";
    private static final String BLUE = "rgba(0, 0, 255, 1)";

    @Test
    public void inheritedStyleInjection() {
        open();
        // Parent of component stylesheet makes all text red
        // Extending class makes it blue
        Assert.assertEquals("Expected child style was not applied.", BLUE,
                findElementById("component").getCssValue("color"));
    }

    @Test
    public void inheritedHtmlInjection() {
        open();

        // Initial HTML import logs a message on the page
        List<String> messages = getMessages();

        Assert.assertEquals(2, messages.size());
        Assert.assertEquals(messages.get(0),
                "Messagehandler initialized in HTML import 1");
        Assert.assertEquals(messages.get(1),
                "Messagehandler initialized in HTML import 2");
    }

    @Test
    public void inheritedScriptInjection() {
        open();

        findElementById("addJs").click();
        // Initial HTML import logs a message on the page
        List<String> messages = getMessages();

        Assert.assertEquals(4, messages.size());
        Assert.assertEquals(messages.get(0),
                "Messagehandler initialized in HTML import 1");
        Assert.assertEquals(messages.get(1),
                "Messagehandler initialized in HTML import 2");
        Assert.assertEquals(messages.get(2), "script1 is loaded");
        Assert.assertEquals(messages.get(3), "script2 is loaded");

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
