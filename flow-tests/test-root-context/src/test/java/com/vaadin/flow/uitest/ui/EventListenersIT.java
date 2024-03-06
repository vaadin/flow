/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class EventListenersIT extends ChromeBrowserTest {

    @Test
    public void clickListenerIsCalledOnlyOnce() {
        open();

        WebElement button = findElement(By.id("click"));
        button.click();

        List<WebElement> clicks = findElements(By.className("count"));
        Assert.assertEquals(1, clicks.size());

        Assert.assertEquals("1", clicks.get(0).getText());

        button.click();

        clicks = findElements(By.className("count"));

        Assert.assertEquals(2, clicks.size());
        Assert.assertEquals("1", clicks.get(0).getText());
        Assert.assertEquals("2", clicks.get(1).getText());
    }
}
