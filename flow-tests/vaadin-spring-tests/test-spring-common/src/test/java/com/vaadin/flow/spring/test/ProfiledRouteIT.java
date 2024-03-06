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

public class ProfiledRouteIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/profiled";
    }

    @Test
    public void profiledRouteIsEnabled() {
        open();

        Assert.assertTrue(
                "Couldn't find a navigation target with enabled profile",
                isElementPresent(By.id("profiled-enabled")));
    }
}
