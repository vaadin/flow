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

public class WaitForVaadinIT extends ChromeBrowserTest {
    @Test
    public void testWaitForVaadin() {
        open();

        WebElement message = findElement(By.id("message"));
        WebElement button = findElement(By.tagName("button"));

        Assert.assertEquals("Not updated", message.getText());

        button.click();

        Assert.assertEquals("Updated", message.getText());
    }
}
