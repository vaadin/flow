/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PreserveOnRefreshNavigationIT extends ChromeBrowserTest {

    @Test
    public void routerLink_selfNavigationWithQueryParams_urlChanges() {
        open();
        $(TestBenchElement.class).id("link-one").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=one"));
        $(TestBenchElement.class).id("link-two").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=two"));
        $(TestBenchElement.class).id("link-three").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=three"));
    }

    @Test
    public void programmaticNavigation_selfNavigationWithQueryParams_urlChanges() {
        open();
        $(TestBenchElement.class).id("button-one").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=one"));
        $(TestBenchElement.class).id("button-two").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=two"));
        $(TestBenchElement.class).id("button-three").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=three"));
    }

}
