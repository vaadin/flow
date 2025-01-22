/*
 * Copyright 2000-2025 Vaadin Ltd.
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
    public void inheritedModuleInjection() {
        open();

        List<String> messages = getMessages();

        int index = messages.indexOf("Messagehandler initialized in module 1");
        Assert.assertTrue("Js Module is not found on the page", index >= 0);

        Assert.assertEquals("Messagehandler initialized in module 2",
                messages.get(index + 1));
    }

    @Test
    public void inheritedScriptInjection() {
        open();

        List<String> messages = getMessages();

        int index = messages.indexOf("script1 is loaded");
        Assert.assertTrue("Js Module is not found on the page", index >= 0);

        Assert.assertEquals("script2 is loaded", messages.get(index + 1));
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
