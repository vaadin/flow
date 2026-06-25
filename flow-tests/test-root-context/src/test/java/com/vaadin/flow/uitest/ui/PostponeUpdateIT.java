/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PostponeUpdateIT extends ChromeBrowserTest {

    @Test
    public void postpone_continueKeepsCurrentUrl() {
        open();

        findElement(By.id("link")).click();
        $(NativeButtonElement.class).id("proceedButton").click();

        String updatedUrl = getDriver().getCurrentUrl();

        // navigate again to see that we do not revert to url we came in with.
        findElement(By.id("link")).click();

        waitUntil(driver -> isElementPresent(By.id("proceedButton")));

        Assert.assertTrue(String.format(
                "Before proceed, the URL in the address bar should stay as %s. But, it was %s",
                updatedUrl, getDriver().getCurrentUrl()),
                getDriver().getCurrentUrl().equals(updatedUrl));
        String currentTarget = findElement(By.id("link")).getText();

        $(NativeButtonElement.class).id("proceedButton").click();

        Assert.assertFalse(
                "Proceeding should have updated parameter from "
                        + currentTarget,
                getDriver().getCurrentUrl().endsWith(currentTarget));
    }

    @Test
    public void postpone_cancelResetsUrlOnBack() {
        open();

        findElement(By.id("link")).click();
        $(NativeButtonElement.class).id("proceedButton").click();

        String updatedUrl = getDriver().getCurrentUrl();

        // Go back to previous
        getDriver().navigate().back();
        waitUntil(driver -> isElementPresent(By.id("cancelButton")));

        $(NativeButtonElement.class).id("cancelButton").click();

        Assert.assertTrue("Canceling should revert to " + updatedUrl,
                getDriver().getCurrentUrl().equals(updatedUrl));
    }

}
