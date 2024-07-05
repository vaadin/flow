/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class MyThemeIT extends ChromeBrowserTest {

    @Test
    public void loadBaseComponent() {
        getDriver().get(getRootURL()
                + "/view/com.vaadin.flow.uitest.ui.theme.MyComponentView");

        TestBenchElement element = $("my-component").first();

        Assert.assertNotNull("Couldn't find element.",
                element.$("*").id("component"));
    }

    @Test
    public void loadThemeComponent() {
        getDriver().get(getRootURL()
                + "/view/com.vaadin.flow.uitest.ui.theme.MyThemeComponentView");

        Assert.assertTrue(findElement(By.id("theme-component")).isDisplayed());
    }

}
