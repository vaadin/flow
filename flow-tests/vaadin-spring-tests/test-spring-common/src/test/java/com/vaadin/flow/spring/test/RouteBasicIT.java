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

public class RouteBasicIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void testServletDeployed() throws Exception {
        open();

        Assert.assertTrue(isElementPresent(By.id("main")));

        findElement(By.id("foo")).click();

        waitUntil(driver -> isElementPresent(By.id("singleton-in-ui")));
    }
}
