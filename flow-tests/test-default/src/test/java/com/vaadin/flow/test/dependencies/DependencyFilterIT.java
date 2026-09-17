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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(DependencyFilterView.class)
public class DependencyFilterIT extends AbstractDefaultIT {

    @BrowserTest
    public void dependenciesLoadedAsExpectedWithFiltering() {
        open();

        waitUntil(input -> !input.findElements(By.className("dependenciesTest"))
                .isEmpty());

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                .map(WebElement::getText).toList();

        assertTrue(testMessages.contains("eager.js"),
                "eager.js should be in the page");

        assertTrue(testMessages.contains(DependencyFilterView.DOM_CHANGE_TEXT),
                "Attach a message via JS should be on the page");

        // The non-existing stylesheet is replaced by filtered.css, which makes
        // the text green
        assertEquals("rgba(0, 128, 0, 1)",
                findElement(By.id("filtered-css")).getCssValue("color"));
    }
}
