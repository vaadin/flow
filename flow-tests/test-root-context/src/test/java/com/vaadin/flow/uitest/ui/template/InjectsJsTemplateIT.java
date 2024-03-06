/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class InjectsJsTemplateIT extends ChromeBrowserTest {

    @Test
    public void executeJsOnInjectedElement() {
        open();

        TestBenchElement parent = $("injects-js-template").first();

        TestBenchElement injectedTemplate = parent.$(TestBenchElement.class)
                .id("injected-template");

        WebElement fooLabel = injectedTemplate.$(TestBenchElement.class)
                .id("foo-prop");
        Assert.assertEquals("bar", fooLabel.getText());

        WebElement bazLabel = injectedTemplate.$(TestBenchElement.class)
                .id("baz-prop");
        Assert.assertEquals("setFromParent", bazLabel.getText());

        WebElement injectedDiv = parent.$(TestBenchElement.class)
                .id("injected-div");
        Assert.assertEquals("setFromParent", injectedDiv.getAttribute("class"));
    }
}
