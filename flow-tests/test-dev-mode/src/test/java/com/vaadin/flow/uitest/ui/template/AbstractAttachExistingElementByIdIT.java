/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public abstract class AbstractAttachExistingElementByIdIT
        extends ChromeBrowserTest {

    protected void assertTemplate(String id, String initialLabelText,
            String placeholder) {
        WebElement input = getInput(id);

        Assert.assertEquals(initialLabelText, getLabel(id).getText());

        Assert.assertEquals(placeholder, input.getAttribute("placeholder"));

        input.sendKeys("Harley!");
        input.sendKeys(Keys.ENTER);

        Assert.assertEquals("Text from input Harley!", getLabel(id).getText());

        // Reset values to defaults
        $(TestBenchElement.class).id(id).$(TestBenchElement.class).id("button")
                .click();

        Assert.assertEquals("default", getLabel(id).getText());
    }

    protected TestBenchElement getInput(String id) {
        return $("*").id(id).$("*").id("input");
    }

    protected TestBenchElement getLabel(String id) {
        return $("*").id(id).$("*").id("label");
    }
}
