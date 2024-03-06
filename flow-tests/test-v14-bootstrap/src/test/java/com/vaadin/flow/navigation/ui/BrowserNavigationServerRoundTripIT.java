/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.navigation.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BrowserNavigationServerRoundTripIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/first";
    }

    @Test
    public void testForwardingToViewInSetParameter() {
        final String baseLoc = "/first";
        getDriver().get(getRootURL() + baseLoc + "/1?query=foo");
        waitForDevServer();

        WebElement button = findElement(By.id(FirstView.PARAM_NAV_BUTTON_ID));
        button.click();

        final String queryValue0 = findElement(By.id(FirstView.QUERY_LABEL_ID))
                .getText();
        Assert.assertEquals("should have received query parameter value 'bar'",
                "query=bar", queryValue0);

        getDriver().navigate().back();

        final String queryValue1 = findElement(By.id(FirstView.QUERY_LABEL_ID))
                .getText();
        Assert.assertEquals("should have received query parameter value 'foo'",
                "query=foo", queryValue1);
    }

    @Test
    public void backAndForwardBrowserButton_triggerServerSideRoundTrip() {
        open();
        waitForDevServer();

        WebElement button = findElement(By.id(FirstView.NAV_BUTTON_ID));
        button.click();

        waitForElementPresent(By.id(SecondView.BUTTON_ID));

        getDriver().navigate().back();

        waitForElementPresent(By.id(FirstView.NAV_BUTTON_ID));

        getDriver().navigate().forward();

        waitForElementPresent(By.id(SecondView.BUTTON_ID));
    }
}
