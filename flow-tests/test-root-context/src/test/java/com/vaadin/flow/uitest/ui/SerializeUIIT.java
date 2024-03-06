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

public class SerializeUIIT extends ChromeBrowserTest {

    @Test
    public void serializeUI() {
        open();

        WebElement serialize = findElement(By.id("serialize"));
        serialize.click();

        WebElement message = findElement(By.id("message"));
        Assert.assertEquals("Successfully serialized ui", message.getText());
    }
}
