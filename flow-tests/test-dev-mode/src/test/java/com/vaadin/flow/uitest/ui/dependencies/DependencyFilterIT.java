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
package com.vaadin.flow.uitest.ui.dependencies;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DependencyFilterIT extends ChromeBrowserTest {

    @Test
    public void dependenciesLoadedAsExpectedWithFiltering() {
        open();

        waitUntil(input -> !input.findElements(By.className("dependenciesTest"))
                .isEmpty());

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                .map(WebElement::getText).collect(Collectors.toList());

        boolean found = testMessages.stream()
                .anyMatch(message -> message.equals("eager.js"));
        Assert.assertTrue("eager.js should be in the page", found);

        found = testMessages.stream().anyMatch(message -> message
                .equals(DependenciesLoadingBaseView.DOM_CHANGE_TEXT));
        Assert.assertTrue("Attach a message via JS should be on the page",
                found);

        WebElement filteredElement = findElement(By.id("filtered-css"));
        String color = filteredElement.getCssValue("color");
        Assert.assertEquals("rgba(0, 128, 0, 1)", color);
    }

}
