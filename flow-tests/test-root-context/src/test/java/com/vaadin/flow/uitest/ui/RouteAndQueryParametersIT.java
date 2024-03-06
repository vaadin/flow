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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RouteAndQueryParametersIT extends ChromeBrowserTest {

    @Test
    public void testNavigationWithBothRouteAndQueryParameters() {
        open();

        Assert.assertFalse(getDriver().getPageSource().contains("foo=bar"));

        findElement(By.id("both")).click();

        Assert.assertTrue(getDriver().getPageSource()
                .contains("route parameter: 5, query string:foo=bar"));

        findElement(By.id("qponly")).click();

        Assert.assertTrue(getDriver().getPageSource()
                .contains("route parameter: null, query string:foo=bar"));

    }

}
