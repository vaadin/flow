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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshShortcutIT extends ChromeBrowserTest {

    @Test
    public void replaceComponentAfterRefresh_componentIsReplaced() {
        open();

        waitPageLoad();

        new Actions(getDriver()).sendKeys(Keys.ENTER).build().perform();
        List<WebElement> infos = findElements(By.className("info"));

        Assert.assertEquals(1, infos.size());
        Assert.assertEquals("Clicked", infos.get(0).getText());

        open();

        waitPageLoad();

        new Actions(getDriver()).sendKeys(Keys.ENTER).build().perform();
        infos = findElements(By.className("info"));

        Assert.assertEquals(2, infos.size());
        Assert.assertEquals("Clicked", infos.get(1).getText());

    }

    private void waitPageLoad() {
        waitUntil(driver -> !findElement(By.className("v-loading-indicator"))
                .isDisplayed());

        waitForElementVisible(By.id("trigger"));
    }

}
