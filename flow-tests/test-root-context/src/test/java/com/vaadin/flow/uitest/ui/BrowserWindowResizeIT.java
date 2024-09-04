/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BrowserWindowResizeIT extends ChromeBrowserTest {

    @Test
    public void listenResizeEvent() {
        open();
        Dimension currentSize = getDriver().manage().window().getSize();
        int newWidth = currentSize.getWidth() - 10;
        getDriver().manage().window()
                .setSize(new Dimension(newWidth, currentSize.getHeight()));

        WebElement info = findElement(By.id("size-info"));
        waitForWidthChange(info, currentSize.getWidth());

        Assert.assertEquals(String.valueOf(newWidth), info.getText());

        currentSize = getDriver().manage().window().getSize();
        newWidth -= 30;
        getDriver().manage().window()
                .setSize(new Dimension(newWidth, currentSize.getHeight()));
        waitForWidthChange(info, currentSize.getWidth());
        Assert.assertEquals(String.valueOf(newWidth), info.getText());
    }

    private void waitForWidthChange(WebElement info, int currentWidth) {
        waitUntil(d -> !String.valueOf(currentWidth).equals(info.getText()));
    }
}
