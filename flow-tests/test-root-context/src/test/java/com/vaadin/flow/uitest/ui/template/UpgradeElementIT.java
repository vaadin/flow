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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class UpgradeElementIT extends ChromeBrowserTest {

    @Test
    public void twoWayDatabindingForUpgradedElement() {
        open();

        findElement(By.id("upgrade")).click();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        WebElement input = template.$(TestBenchElement.class).id("input");
        new Actions(getDriver()).click(input).sendKeys("foo")
                .sendKeys(Keys.ENTER).build().perform();
        WebElement result = findElement(By.id("text-update"));
        Assert.assertEquals("foo", result.getText());
    }
}
