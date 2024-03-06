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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplateInTemplateIT extends ChromeBrowserTest {

    @Test
    public void childTemplateInstanceHandlesEvent() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement child = template.$(TestBenchElement.class).id("child");

        child.getPropertyElement("shadowRoot", "firstElementChild").click();

        Assert.assertTrue(isElementPresent(By.id("click-handler")));

        WebElement text = child.$(TestBenchElement.class).id("text");
        Assert.assertEquals("foo", text.getText());
    }
}
