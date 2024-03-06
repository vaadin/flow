/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import org.junit.Before;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.flow.testutil.ChromeDeviceTest;

public class BasicComponentIT extends ChromeDeviceTest {
    @Before
    public void init() {
        getDriver().get(getRootURL());
        waitForDevServer();
        getDriver().get(getRootURL() + "/basic-component.html");
    }

    @Test
    public void componentIsLoaded() {
        checkLogsForErrors();

        TestBenchElement component = $("basic-component").first();
        TestBenchElement h1 = component.$("h1").first();
        Assert.assertEquals("Basic Component", h1.getText());
    }

    @Test
    public void themeIsAppliedToComponent() {
        checkLogsForErrors();

        TestBenchElement component = $("basic-component").first();
        TestBenchElement h1 = component.$("h1").first();
        Assert.assertEquals("rgba(255, 0, 0, 1)", h1.getCssValue("color"));
    }
}
