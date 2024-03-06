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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class CustomCustomElementIT extends ChromeBrowserTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void clickOnButton_removeFromLayout() {
        TestBenchElement customElement = $("custom-custom-element").first();

        Assert.assertEquals("initial",
                customElement.getPropertyString("shadowRoot", "textContent"));

        findElement(By.tagName("button")).click();

        Assert.assertEquals("updated",
                customElement.getPropertyString("shadowRoot", "textContent"));
    }

}
