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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class JsApiGetByIdIT extends ChromeBrowserTest {

    @Test
    public void getByNodeId_JsAPI() {
        open();

        WebElement source = findElement(By.id("source"));
        String text = source.getText();
        WebElement target = findElement(By.id("target"));

        Assert.assertNotEquals(text, target.getText());

        findElement(By.id("update")).click();

        Assert.assertEquals(text, target.getText());
    }
}
