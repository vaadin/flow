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

public class CompositeNestedIT extends ChromeBrowserTest {

    @Test
    public void testBasics() {
        open();
        WebElement name = findElement(By.id(CompositeNestedView.NAME_ID));
        InputTextElement input = $(InputTextElement.class)
                .id(CompositeNestedView.NAME_FIELD_ID);
        Assert.assertEquals("Name on server:", name.getText());
        input.setValue("123");
        Assert.assertEquals("Name on server: 123", name.getText());
    }
}
