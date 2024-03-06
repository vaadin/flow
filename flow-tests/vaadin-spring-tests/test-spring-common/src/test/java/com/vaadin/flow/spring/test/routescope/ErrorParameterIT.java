/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.spring.test.AbstractSpringTest;

public class ErrorParameterIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/throw-exception";
    }

    @Test
    public void navigateToViewWhichThrows_beansInsideErrorViewArePreservedinScope() {
        open();

        Assert.assertTrue(isElementPresent(By.id("custom-exception-created")));
        Assert.assertTrue(
                isElementPresent(By.id("custom-exception-destroyed")));

        WebElement button = findElement(By.id("custom-exception-button"));
        String buttonId = button.getText();

        switchContent();

        WebElement div = findElement(By.id("custom-exception-div"));
        String divId = div.getText();

        switchContent();

        Assert.assertEquals("Button sub component is not preserved", buttonId,
                findElement(By.id("custom-exception-button")).getText());

        switchContent();

        Assert.assertEquals("Div sub component is not preserved", divId,
                findElement(By.id("custom-exception-div")).getText());
    }

    private void switchContent() {
        findElement(By.id("switch-content")).click();
    }

}
