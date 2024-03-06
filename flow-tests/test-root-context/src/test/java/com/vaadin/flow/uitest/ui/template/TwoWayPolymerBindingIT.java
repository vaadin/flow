/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TwoWayPolymerBindingIT extends ChromeBrowserTest {

    @Test
    public void initialModelValueIsPresentAndModelUpdatesNormally() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        WebElement input = template.$(TestBenchElement.class).id("input");

        // The initial client-side value should be sent from the client to the
        // model
        waitUntil(driver -> "Value: foo".equals(getStatusMessage()));

        // now make explicit updates from the client side
        input.clear();
        input.sendKeys("a");
        Assert.assertEquals("Value: a", getStatusMessage());

        input.sendKeys("b");
        Assert.assertEquals("Value: ab", getStatusMessage());

        // Reset the model value from the server-side
        template.$(TestBenchElement.class).id("reset").click();
        Assert.assertEquals("Value:", getStatusMessage());
        Assert.assertEquals("", getValueProperty(input));

        input.sendKeys("c");
        Assert.assertEquals("Value: c", getStatusMessage());
    }

    private Object getValueProperty(WebElement input) {
        return ((JavascriptExecutor) getDriver())
                .executeScript("return arguments[0].value", input);
    }

    private String getStatusMessage() {
        TestBenchElement template = $(TestBenchElement.class).id("template");

        return template.$(TestBenchElement.class).id("status").getText();
    }
}
