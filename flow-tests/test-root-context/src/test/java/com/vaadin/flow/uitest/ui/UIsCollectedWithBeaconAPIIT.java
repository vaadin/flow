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

public class UIsCollectedWithBeaconAPIIT extends ChromeBrowserTest {

    @Test
    public void beaconHandling_navigateAwayFromApplication_uiClosedEarly() {
        openUiAndExpect1();
        goToGoogle();
        // If previous UI is not properly detached, following will fail
        openUiAndExpect1();
    }

    private void openUiAndExpect1() throws NumberFormatException {
        open();
        WebElement uisCount = findElement(By.id("uis"));
        int count = Integer.parseInt(uisCount.getText());
        Assert.assertEquals(1, count);
    }

    private void goToGoogle() {
        getDriver().get("https://google.com/");
        Assert.assertTrue(getDriver().getPageSource().contains("Google"));
    }

}
