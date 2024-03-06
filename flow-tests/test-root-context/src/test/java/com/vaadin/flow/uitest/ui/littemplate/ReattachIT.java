/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ReattachIT extends ChromeBrowserTest {

    @Test
    public void reattachedTemplateHasExplicitlySetText() {
        open();

        WebElement button = findElement(By.id("click"));

        // attach template
        button.click();

        TestBenchElement template = $(TestBenchElement.class)
                .id("form-template");
        TestBenchElement div = template.$(TestBenchElement.class).id("div");

        Assert.assertEquals("foo", div.getText());

        // detach template
        button.click();

        // re-attach template
        button.click();

        template = $(TestBenchElement.class).id("form-template");
        div = template.$(TestBenchElement.class).id("div");
        Assert.assertEquals("foo", div.getText());
    }

}
