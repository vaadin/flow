/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.dependencies;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(OrderedDependencyView.class)
public class OrderedDependencyIT extends AbstractDefaultIT {

    private static final String BLUE = "rgba(0, 0, 255, 1)";

    @BrowserTest
    public void inheritedStyleInjection() {
        open();
        // Parent of component stylesheet makes all text red
        // Extending class makes it blue
        Assertions.assertEquals(BLUE,
                findElement(By.id("component")).getCssValue("color"),
                "Expected child style was not applied.");
    }

    @BrowserTest
    public void inheritedModuleInjection() {
        open();

        List<String> messages = getMessages();

        int index = messages.indexOf("Messagehandler initialized in module 1");
        assertTrue(index >= 0, "Js Module is not found on the page");

        Assertions.assertEquals("Messagehandler initialized in module 2",
                messages.get(index + 1));
    }

    @BrowserTest
    public void inheritedScriptInjection() {
        open();

        List<String> messages = getMessages();

        int index = messages.indexOf("script1 is loaded");
        assertTrue(index >= 0, "Js Module is not found on the page");

        Assertions.assertEquals("script2 is loaded", messages.get(index + 1));
    }

    private List<String> getMessages() {
        return findElements(By.className("message")).stream()
                .map(WebElement::getText).toList();
    }
}
