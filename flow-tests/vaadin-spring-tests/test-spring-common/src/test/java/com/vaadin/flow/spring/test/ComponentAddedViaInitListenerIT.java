/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;
import org.openqa.selenium.By;

public class ComponentAddedViaInitListenerIT extends AbstractSpringTest {

    @Test
    public void componentAddedViaInitListenerIsLoaded() {
        open();

        TestBenchElement component = $("init-listener-component").first();
        TestBenchElement div = component.$("div").first();
        Assert.assertEquals("Init Listener Component", div.getText());

        // Ensure the class name set by @EventListener style listener is there
        getDriver().findElement(By.cssSelector(".event-listener-was-here"));
    }

    @Override
    protected String getTestPath() {
        return "/";
    }
}
