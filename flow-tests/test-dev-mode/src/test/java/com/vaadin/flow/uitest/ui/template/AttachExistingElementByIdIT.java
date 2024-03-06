/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

public class AttachExistingElementByIdIT extends ChromeBrowserTest {

    @Test
    public void elementsAreBoundOnTheServerSide() {
        open();

        assertTemplate("simple-path");
    }

    private void assertTemplate(String id) {
        assertTemplate(id, "default", "Type here to update label");
    }

    private void assertTemplate(String id, String initialLabelText,
            String placeholder) {
        TestBenchElement template = $("*").id(id);
        WebElement input = getInput(template);

        Assert.assertEquals(initialLabelText, getLabel(template).getText());

        Assert.assertEquals(placeholder, input.getAttribute("placeholder"));

        input.sendKeys("Harley!");
        input.sendKeys(Keys.ENTER);

        Assert.assertEquals("Text from input Harley!",
                getLabel(template).getText());

        // Reset values to defaults
        $(TestBenchElement.class).id(id).$(TestBenchElement.class).id("button")
                .click();

        Assert.assertEquals("default", getLabel(template).getText());
    }

    private WebElement getInput(TestBenchElement template) {
        return template.$("*").id("input");
    }

    private WebElement getLabel(TestBenchElement template) {
        return template.$("*").id("label");
    }
}
