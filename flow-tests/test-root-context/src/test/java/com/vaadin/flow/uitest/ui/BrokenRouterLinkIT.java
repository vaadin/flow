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
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BrokenRouterLinkIT extends ChromeBrowserTest {

    // https://github.com/vaadin/flow/issues/8544
    @Test
    public void testRouterLink_linkIsBroken_urlIsUpdated() {
        open();

        // enable after https://github.com/vaadin/vaadin-router/issues/43 is
        // fixed
        Assume.assumeFalse(isClientRouter());

        WebElement link = findElement(By.id(BrokenRouterLinkView.LINK_ID));

        String href = link.getAttribute("href");

        link.click();

        Assert.assertTrue(getDriver().getCurrentUrl().endsWith(href));
    }

    // https://github.com/vaadin/flow/issues/8693
    @Test
    public void testRouterLink_visitBrokenLinkAndBack_scrollPositionIsRetained() {
        open();

        // enable after https://github.com/vaadin/vaadin-router/issues/43 is
        // fixed
        Assume.assumeFalse(isClientRouter());

        executeScript("window.scrollTo(0,100)");

        WebElement link = findElement(By.id(BrokenRouterLinkView.LINK_ID));
        link.click();

        long y0 = (Long) executeScript("return window.scrollY");
        Assert.assertEquals(0L, y0);

        getDriver().navigate().back();

        waitForElementPresent(By.id(BrokenRouterLinkView.LINK_ID));

        long y1 = (Long) executeScript("return window.scrollY");
        Assert.assertEquals(100L, y1);
    }
}
