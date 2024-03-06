/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NavigationTargetIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void testServletDeployed() throws Exception {
        open();

        WebElement navigationTarget = findElement(By.id("navigationTarget"));

        Assert.assertNotNull(navigationTarget);
        Assert.assertEquals("Hello world", navigationTarget.getText());
    }
}
