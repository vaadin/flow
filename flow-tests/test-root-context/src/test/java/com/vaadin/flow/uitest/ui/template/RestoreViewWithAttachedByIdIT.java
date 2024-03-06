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

public class RestoreViewWithAttachedByIdIT extends ChromeBrowserTest {

    @Test
    public void injectedComponentWorksAfterReattach() {
        open();

        WebElement target = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("target");
        Assert.assertEquals("Server Side Text", target.getText());

        // replace the template with a label
        WebElement button = findElement(By.tagName("button"));
        button.click();

        Assert.assertTrue(isElementPresent(By.id("info")));

        // return the template back
        button.click();

        target = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("target");
        Assert.assertEquals("Server Side Text", target.getText());
    }
}
