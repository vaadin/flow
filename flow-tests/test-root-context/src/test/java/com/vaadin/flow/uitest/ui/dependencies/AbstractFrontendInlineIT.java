/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public abstract class AbstractFrontendInlineIT extends ChromeBrowserTest {

    @Test
    public void inlineDependeciesWithFrontendProtocol() {
        open();

        checkLogsForErrors(msg -> msg.contains("HTML Imports is deprecated"));

        waitUntil(driver -> !driver
                .findElement(By.className("v-loading-indicator"))
                .isDisplayed());

        WebElement templateElement = $(TestBenchElement.class).id("template")
                .$(DivElement.class).id("frontend-inline");

        Assert.assertEquals("Inline HTML loaded via frontent protocol",
                templateElement.getText());

        String color = templateElement.getCssValue("color");
        Assert.assertEquals("rgba(0, 128, 0, 1)", color);

        WebElement js = findElement(By.id("js"));
        Assert.assertEquals("Inlined JS", js.getText());
    }

}
