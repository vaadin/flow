/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testcategory.SlowTests;

@Category(SlowTests.class)
public class InfiniteRerouteLoopIT extends ChromeBrowserTest {

    private static final String NAVIGATION_EXCEPTION = "navigation-exception";

    @Test
    public void renderNavigationExceptionTarget_locationIsNotChanged() {
        open();

        waitUntil(driver -> isElementPresent(By.tagName("body")));

        Assert.assertTrue(
                driver.getCurrentUrl().endsWith(NAVIGATION_EXCEPTION));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        int index = path.lastIndexOf("/");
        return path.substring(0, index + 1) + NAVIGATION_EXCEPTION;
    }

    @Override
    protected Class<? extends Component> getViewClass() {
        return PushRouteNotFoundView.class;
    }

}
