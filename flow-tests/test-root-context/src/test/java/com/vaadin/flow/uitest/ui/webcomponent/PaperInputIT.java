/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.webcomponent;

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PaperInputIT extends ChromeBrowserTest {

    @Test
    public void paperInputIsFunctional() {
        open();

        WebElement input = findElement(By.tagName("paper-input"));
        String originalValue = input.getAttribute("value");
        input.sendKeys(Keys.END + "bar");

        List<WebElement> updateValueElements = findElements(
                By.className("update-value"));
        WebElement lastUpdateValue = updateValueElements
                .get(updateValueElements.size() - 1);
        org.junit.Assert.assertEquals(originalValue + "bar",
                lastUpdateValue.getText());
    }
}
