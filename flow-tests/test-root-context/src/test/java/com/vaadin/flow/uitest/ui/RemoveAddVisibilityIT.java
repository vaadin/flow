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

public class RemoveAddVisibilityIT extends ChromeBrowserTest {

    @Test
    public void elementIsVisibleAfterReattach() {
        open();

        WebElement element = findElement(By.tagName("span"));
        Assert.assertEquals(Boolean.TRUE.toString(),
                element.getAttribute("hidden"));

        findElement(By.id("make-visible")).click();

        Assert.assertNull(element.getAttribute("hidden"));
        Assert.assertEquals("Initially hidden", element.getText());
    }
}
