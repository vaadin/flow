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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.By;

public class UIElementIT extends ChromeBrowserTest {

    @Test
    public void uiElementWorksInJSCalls() {
        open();

        List<WebElement> bodyChildrenAddedViaJs = findElements(
                By.className("body-child"));
        Assert.assertEquals(1, bodyChildrenAddedViaJs.size());

        findElement(By.tagName("button")).click();

        bodyChildrenAddedViaJs = findElements(By.className("body-child"));
        Assert.assertEquals(2, bodyChildrenAddedViaJs.size());
    }
}
