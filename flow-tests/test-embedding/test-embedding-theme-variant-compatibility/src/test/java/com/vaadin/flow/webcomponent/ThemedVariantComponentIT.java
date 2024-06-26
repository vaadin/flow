/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ThemedVariantComponentIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void servletPageGetsWebComponent_attributeIsReflectedToServer() {
        open();

        checkLogsForErrors();

        waitUntil(driver -> !driver
                .findElement(By.className("v-loading-indicator"))
                .isDisplayed());

        TestBenchElement webComponent = $("themed-variant-web-component")
                .first();
        Assert.assertEquals("dark", webComponent.getAttribute("theme"));

        String customStyle = $("custom-style").first()
                .getAttribute("innerHTML");
        Assert.assertThat(customStyle,
                CoreMatchers.allOf(
                        CoreMatchers.containsString("theme~=\"dark\""),
                        CoreMatchers.containsString("--lumo-base-color")));
    }
}
