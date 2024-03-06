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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.LabelElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ShadowRootIT extends ChromeBrowserTest {

    @Test
    public void checkShadowRoot() {
        open();

        DivElement div = $(DivElement.class).id("test-element");

        WebElement shadowDiv = div.$(DivElement.class).id("shadow-div");
        Assert.assertEquals("Div inside shadow DOM", shadowDiv.getText());

        WebElement shadowLabel = div.$(LabelElement.class).id("shadow-label");
        Assert.assertEquals("Label inside shadow DOM", shadowLabel.getText());

        findElement(By.id("remove")).click();

        Assert.assertTrue("Child has not been removed from the shadow root",
                findElements(By.id("shadow-label")).isEmpty());
    }
}
