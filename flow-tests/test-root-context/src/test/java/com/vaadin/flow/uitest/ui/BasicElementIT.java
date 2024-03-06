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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class BasicElementIT extends AbstractBasicElementComponentIT {

    // #671, #1231
    @Test
    public void testAddRemoveComponentDuringSameRequest() {
        open();
        findElement(By.id("addremovebutton")).click();

        List<WebElement> addremovecontainerChildren = findElement(
                By.id("addremovecontainer")).findElements(By.tagName("div"));
        Assert.assertEquals(2, addremovecontainerChildren.size());
        Assert.assertEquals("to-remove",
                addremovecontainerChildren.get(0).getAttribute("id"));
        Assert.assertEquals("ok",
                addremovecontainerChildren.get(1).getAttribute("id"));
        // verify the UI still works
        assertDomUpdatesAndEventsDoSomething();
    }
}
