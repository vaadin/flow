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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplateHasInjectedSubTemplateIT extends ChromeBrowserTest {

    @Test
    public void injectedSubTemplate_injectedInstanceWorks()
            throws InterruptedException {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement child = template.$(TestBenchElement.class).id("child");

        TestBenchElement text = child.$(TestBenchElement.class).id("text");

        Assert.assertEquals("foo", text.getText());

        template.$(TestBenchElement.class).id("button").click();
        waitUntil(driver -> "bar".equals(text.getText()));
    }
}
