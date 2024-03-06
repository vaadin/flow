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
import org.openqa.selenium.NoSuchElementException;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.PushLongPollingWithPreserveOnRefreshView.ADD_BUTTON_ID;
import static com.vaadin.flow.uitest.ui.PushLongPollingWithPreserveOnRefreshView.TEST_DIV_ID;
import static com.vaadin.flow.uitest.ui.PushLongPollingWithPreserveOnRefreshView.TEXT_IN_DIV;

@Category(IgnoreOSGi.class)
public class PushLongPollingWithPreserveOnRefreshIT extends ChromeBrowserTest {

    @Test
    public void addDiv_refreshThePage_ensureNoErrorHappensAndDivIsPresent() {

        open();

        waitPageLoad();

        findElement(By.id(ADD_BUTTON_ID)).click();

        ensureDivIsPresent();

        // refresh the browser
        getDriver().navigate().refresh();

        waitPageLoad();

        ensureNoErrorIsDisplayed();

        ensureDivIsPresent();
    }

    private void waitPageLoad() {
        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));
        waitUntil(driver -> !loadingIndicator.isDisplayed());
    }

    private void ensureNoErrorIsDisplayed() {
        Assert.assertThrows(NoSuchElementException.class,
                () -> findElement(By.className("v-system-error")));
    }

    private void ensureDivIsPresent() {
        WebElement div = findElement(By.id(TEST_DIV_ID));

        Assert.assertNotNull(div);
        Assert.assertEquals(TEXT_IN_DIV, div.getText());
    }
}
