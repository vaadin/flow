/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BundledTemplateInTemplateWithIdIT extends ChromeBrowserTest {

    @Test
    public void childTemplateInstanceHandlesEvent() {
        open();

        TestBenchElement template = $("*").id("template");
        TestBenchElement child = template.$("*").id("child");

        WebElement text = child.$("*").id("text");
        Assert.assertEquals("div", text.getTagName());
        Assert.assertEquals("@Id injected!", text.getText());
    }
}
