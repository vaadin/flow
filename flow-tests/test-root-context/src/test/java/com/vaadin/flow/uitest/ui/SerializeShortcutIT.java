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
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class SerializeShortcutIT extends ChromeBrowserTest {

    @Test
    public void addShortcut_UIserializable() {
        open();

        WebElement serialize = findElement(By.id("add-serialize"));
        serialize.click();

        WebElement message = findElement(By.id("message"));
        Assert.assertEquals("Successfully serialized ui", message.getText());
    }

    @Test
    public void addAndRemoveShortcut_UIserializable() {
        open();

        WebElement serialize = findElement(By.id("add-remove-serialize"));
        serialize.click();

        WebElement message = findElement(By.id("message"));
        Assert.assertEquals("Successfully serialized ui", message.getText());
    }

}
