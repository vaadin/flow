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
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class DynamicallyRegisteredRouteIT extends ChromeBrowserTest {

    @Test
    public void testServiceInitListener_canRegisterRoutes() {
        String testURL = getTestURL(getRootURL(), "/view/"
                + TestingServiceInitListener.DYNAMICALLY_REGISTERED_ROUTE,
                null);
        getDriver().get(testURL);
        waitForDevServer();

        List<WebElement> elements = findElements(
                By.id(DynamicallyRegisteredRoute.ID));

        Assert.assertEquals("Route registered during startup is not available",
                1, elements.size());
        Assert.assertEquals("Dynamically registered route not rendered",
                DynamicallyRegisteredRoute.TEXT, elements.get(0).getText());
    }
}
