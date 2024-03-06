/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category({ PushTests.class, IgnoreOSGi.class })
public class PushToggleComponentVisibilityIT extends ChromeBrowserTest {

    private static final String HIDE = "hide";

    @Test
    public void ensureComponentVisible() {
        open();

        $(TestBenchElement.class).id(HIDE).click();
        Assert.assertEquals("Please wait",
                $(TestBenchElement.class).id("label").getText());

        waitUntil(driver -> isElementPresent(By.id(HIDE)));
        $(TestBenchElement.class).id(HIDE).click();
        Assert.assertEquals("Please wait",
                $(TestBenchElement.class).id("label").getText());
    }
}
