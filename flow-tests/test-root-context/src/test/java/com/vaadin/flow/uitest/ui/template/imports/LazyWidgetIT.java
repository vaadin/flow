/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template.imports;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * This test is intended to check that templates work as Polymer elements even
 * if they're lazy loaded.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class LazyWidgetIT extends ChromeBrowserTest {

    @Test
    public void lazyLoadedPolymerTemplateWorksAsElement() {
        open();
        waitForElementVisible(By.id("template")); // template is lazy loaded,
                                                  // need some time to load

        TestBenchElement template = $(TestBenchElement.class).id("template");
        String input = "InputMaster";
        Assert.assertFalse(
                "No greeting should be present before we press the button",
                template.$("*").attribute("id", "greeting").exists());

        template.$(TestBenchElement.class).id("input").sendKeys(input);
        template.$(TestBenchElement.class).id("button").click();

        Assert.assertEquals("Greeting is different from expected",
                String.format(LazyWidgetView.GREETINGS_TEMPLATE, input),
                template.$(TestBenchElement.class).id("greeting").getText());
    }
}
