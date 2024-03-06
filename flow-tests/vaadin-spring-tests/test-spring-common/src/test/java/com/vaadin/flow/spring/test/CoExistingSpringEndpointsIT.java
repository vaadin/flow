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
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

@Category(SpringBootOnly.class)
public class CoExistingSpringEndpointsIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void assertRoutesAndSpringEndpoint() {
        open();

        String nonExistingRoutePath = "non-existing-route";
        Assert.assertTrue(isElementPresent(By.id("main")));

        getDriver().get(getContextRootURL() + '/' + nonExistingRoutePath);

        Assert.assertTrue(getDriver().getPageSource().contains(String
                .format("Could not navigate to '%s'", nonExistingRoutePath)));

        getDriver().get(getContextRootURL() + "/oauth2/authorize");
        // This only asserts that Flow routes do not overwrite other spring
        // paths
        Assert.assertTrue(
                getDriver().getPageSource().contains("type=Bad Request"));
    }
}
