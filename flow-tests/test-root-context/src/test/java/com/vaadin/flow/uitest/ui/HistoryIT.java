/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class HistoryIT extends ChromeBrowserTest {

    @Test
    public void testHistory() throws URISyntaxException {
        open();

        URI baseUrl = getCurrentUrl();

        InputTextElement stateField = $(InputTextElement.class).id("state");
        InputTextElement locationField = $(InputTextElement.class)
                .id("location");
        WebElement pushButton = findElement(By.id("pushState"));
        WebElement replaceButton = findElement(By.id("replaceState"));
        WebElement backButton = findElement(By.id("back"));
        WebElement forwardButton = findElement(By.id("forward"));
        WebElement clearButton = findElement(By.id("clear"));

        stateField.setValue("{'foo':true}");
        locationField.setValue("asdf");
        pushButton.click();

        Assert.assertEquals(baseUrl.resolve("asdf"), getCurrentUrl());

        // Back to original state
        backButton.click();

        Assert.assertEquals(baseUrl, getCurrentUrl());
        Assert.assertEquals(
                Arrays.asList(
                        "New location: com.vaadin.flow.uitest.ui.HistoryView"),
                getStatusMessages());
        clearButton.click();

        stateField.clear();
        locationField.clear();
        locationField.setValue("qwerty");
        replaceButton.click();

        Assert.assertEquals(baseUrl.resolve("qwerty"), getCurrentUrl());

        // Forward to originally pushed state
        forwardButton.click();
        Assert.assertEquals(baseUrl.resolve("asdf"), getCurrentUrl());
        Assert.assertEquals(Arrays.asList("New location: asdf",
                "New state: {\"foo\":true}"), getStatusMessages());
        clearButton.click();

        // Back to the replaced state
        backButton.click();

        Assert.assertEquals(baseUrl.resolve("qwerty"), getCurrentUrl());
        Assert.assertEquals(Arrays.asList("New location: qwerty"),
                getStatusMessages());

        // Navigate to empty string should go to the context path root
        stateField.clear();
        locationField.clear();
        pushButton.click();

        Assert.assertEquals(baseUrl.resolve("."), getCurrentUrl());
    }

    private URI getCurrentUrl() throws URISyntaxException {
        URI uri = new URI(getDriver().getCurrentUrl());
        return uri;
    }

    private List<String> getStatusMessages() {
        return findElements(By.className("status")).stream()
                .map(WebElement::getText).collect(Collectors.toList());
    }
}
