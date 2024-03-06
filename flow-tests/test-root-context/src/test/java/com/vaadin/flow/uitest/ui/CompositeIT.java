/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class CompositeIT extends ChromeBrowserTest {

    @Test
    public void changeOnClient() {
        open();
        WebElement name = findElement(By.id(CompositeNestedView.NAME_ID));
        InputTextElement input = $(InputTextElement.class)
                .id(CompositeNestedView.NAME_FIELD_ID);
        Assert.assertEquals("Name on server:", name.getText());
        input.setValue("123");
        Assert.assertEquals("Name on server: 123", name.getText());

        InputTextElement serverValueInput = $(InputTextElement.class)
                .id(CompositeView.SERVER_INPUT_ID);
        WebElement serverValueButton = findElement(
                By.id(CompositeView.SERVER_INPUT_BUTTON_ID));

        serverValueInput.setValue("server value");
        serverValueButton.click();

        Assert.assertEquals("Name on server: server value", name.getText());

    }

    @Test
    public void htmlImportOfContentLoaded() {
        open();
        waitForElementPresent(By.id(CompositeView.COMPOSITE_PAPER_SLIDER));
        TestBenchElement paperSlider = (TestBenchElement) findElement(
                By.id(CompositeView.COMPOSITE_PAPER_SLIDER));
        Assert.assertEquals("100", paperSlider.getPropertyString("max"));

    }
}
