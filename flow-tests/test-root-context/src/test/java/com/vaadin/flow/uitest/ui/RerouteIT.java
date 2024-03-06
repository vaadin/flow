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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RerouteIT extends ChromeBrowserTest {

    @Test
    public void testReroutingToErrorView() {
        open();
        WebElement checkbox = findElement(By.id("check"))
                .findElement(By.tagName("input"));
        checkbox.click();

        findElement(By.id("navigate")).click();

        Assert.assertTrue(
                getDriver().getPageSource().contains("Could not navigate to "));
    }

    @Test
    public void testViewWithoutRerouting() {
        open();
        findElement(By.id("navigate")).click();

        Assert.assertNotNull("Navigate button was not found",
                findElement(By.id("navigate")));
    }
}
