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
package com.vaadin.flow.uitest.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class HistoryIT extends ChromeBrowserTest {

    @Test
    public void testHistory() throws URISyntaxException {
        open();

        URI baseUrl = new URI(getDriver().getCurrentUrl());

        InputTextElement stateField = $(InputTextElement.class).id("state");
        InputTextElement locationField = $(InputTextElement.class)
                .id("location");
        WebElement pushButton = findElement(By.id("pushState"));
        WebElement replaceButton = findElement(By.id("replaceState"));
        WebElement backButton = findElement(By.id("back"));
        WebElement forwardButton = findElement(By.id("forward"));
        WebElement clearButton = findElement(By.id("clear"));

        stateField.setValue("{\"foo\":true}");
        locationField.setValue("asdf");
        pushButton.click();

        waitForUrl(baseUrl.resolve("asdf"));

        // Back to original state
        backButton.click();

        waitForUrl(baseUrl);
        // idx value in history state is added by react-router
        waitForStatusMessages(
                "New location: com.vaadin.flow.uitest.ui.HistoryView");
        clearButton.click();

        stateField.clear();
        locationField.clear();
        locationField.setValue("qwerty");
        replaceButton.click();

        waitForUrl(baseUrl.resolve("qwerty"));

        // Forward to originally pushed state
        forwardButton.click();
        waitForUrl(baseUrl.resolve("asdf"));
        waitForStatusMessages("New location: asdf",
                "New state: {\"foo\":true}");
        clearButton.click();

        // Back to the replaced state
        backButton.click();

        waitForUrl(baseUrl.resolve("qwerty"));
        waitForStatusMessages("New location: qwerty");

        // Navigate to empty string should go to the context path root
        stateField.clear();
        locationField.setValue("qwerty/x");
        pushButton.click();
        locationField.clear();
        pushButton.click();

        URI expectedRoot = baseUrl.resolve(".");
        waitForUrl(expectedRoot);

        // Replacing with empty string should go to the context path root
        locationField.setValue("qwerty/x");
        replaceButton.click();
        locationField.clear();
        replaceButton.click();
        waitForUrl(expectedRoot);
    }

    private void waitForUrl(URI expected) {
        waitUntil(arg -> {
            try {
                return expected.equals(new URI(getDriver().getCurrentUrl()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void waitForStatusMessages(String... expectedMessages) {
        List<String> expected = Arrays.asList(expectedMessages);
        waitUntil(arg -> expected.equals(findElements(By.className("status"))
                .stream().map(WebElement::getText)
                .collect(Collectors.toList())));
    }
}
