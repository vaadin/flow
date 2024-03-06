/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public abstract class AbstractContextInlineIT extends ChromeBrowserTest {

    @Test
    public void inlineDependeciesWithFrontendProtocol() {
        open();

        WebElement templateElement = $(TestBenchElement.class).id("template");

        String color = templateElement.getCssValue("color");
        Assert.assertEquals("rgba(0, 128, 0, 1)", color);

        WebElement js = findElement(By.id("js"));
        Assert.assertEquals("Inlined JS", js.getText());
    }

}
