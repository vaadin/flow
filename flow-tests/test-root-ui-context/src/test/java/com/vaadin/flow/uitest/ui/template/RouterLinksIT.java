/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class RouterLinksIT extends ChromeBrowserTest {

    public static final String TEXT_INPUT = "abc";

    @Test
    public void handleEventOnServer() {
        open();

        String originalUrl = getDriver().getCurrentUrl();
        WebElement textInput = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("input");

        Assert.assertTrue("Input was not empty",
                textInput.getAttribute("value").isEmpty());
        textInput.sendKeys(TEXT_INPUT);
        Assert.assertEquals("Input was missing contents", TEXT_INPUT,
                textInput.getAttribute("value"));
        List<TestBenchElement> links = $(TestBenchElement.class).id("template")
                .$("a").all();
        WebElement link = links.stream()
                .filter(lnk -> lnk.getText().equals("Navigate")).findFirst()
                .get();

        getCommandExecutor().executeScript("return arguments[0].click()", link);

        // Original url should end with UI and the navigation link Template
        Assert.assertNotEquals(originalUrl, getDriver().getCurrentUrl());

        textInput = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("input");

        Assert.assertEquals("Input didn't keep content through navigation",
                TEXT_INPUT, textInput.getAttribute("value"));
    }
}
