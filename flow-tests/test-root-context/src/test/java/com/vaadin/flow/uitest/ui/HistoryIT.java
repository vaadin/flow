/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
        locationField.setValue("qwerty/x");
        pushButton.click();
        locationField.clear();
        pushButton.click();

        Assert.assertEquals(baseUrl.resolve("."), getCurrentUrl());

        // Replacing with empty string should go to the context path root
        locationField.setValue("qwerty/x");
        replaceButton.click();
        locationField.clear();
        replaceButton.click();
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
