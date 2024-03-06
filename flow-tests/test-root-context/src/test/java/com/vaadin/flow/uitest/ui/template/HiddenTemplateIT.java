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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class HiddenTemplateIT extends ChromeBrowserTest {

    @Test
    public void initiallyHiddenElementStaysHidden() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        WebElement child = template.$(TestBenchElement.class)
                .id("hidden-child");
        Assert.assertNotNull(child.getAttribute("hidden"));

        WebElement visibility = template.$(TestBenchElement.class)
                .id("visibility");
        visibility.click();
        Assert.assertNotNull(child.getAttribute("hidden"));

        visibility.click();
        Assert.assertNotNull(child.getAttribute("hidden"));
    }

    @Test
    public void initiallyNotHiddenElementChangesItsVisibility() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");

        WebElement child = template.$(TestBenchElement.class).id("child");
        Assert.assertNull(child.getAttribute("hidden"));

        WebElement visibility = template.$(TestBenchElement.class)
                .id("visibility");
        visibility.click();
        Assert.assertNotNull(child.getAttribute("hidden"));

        visibility.click();
        Assert.assertNull(child.getAttribute("hidden"));
    }
}
