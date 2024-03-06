/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class NPEHandlerIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/npe";
    }

    @Test
    public void npeIsHandledByComponent() {
        open();

        Assert.assertTrue(
                "Couldn't find element from the component which represents error for "
                        + NullPointerException.class.getName(),
                isElementPresent(By.id("npe-handle")));
    }

    @Test
    public void noRouteIsHandledByExistingFlowComponent() {
        // This will wait for dev server to load first
        open();

        String nonExistingRoutePath = "non-existing-route";
        getDriver().get(getTestURL(getRootURL(),
                getContextPath() + '/' + nonExistingRoutePath, new String[0]));

        Assert.assertTrue(getDriver().getPageSource().contains(String
                .format("Could not navigate to '%s'", nonExistingRoutePath)));
    }
}
