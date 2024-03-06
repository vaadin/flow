/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class UpdatePropertyIT extends ChromeBrowserTest implements HasById {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/updateProperty.html";
    }

    @Test
    public void propertiesWrittenOnServerSideAreUpdatedToWebComponent() {
        open();

        waitForElementVisible(By.id("counter"));

        TestBenchElement button = byId("counter").$("button").first();

        Assert.assertEquals("Count (id:count) should start from 0", 0,
                getInt("count"));
        Assert.assertEquals("Count (id:json) should start from 0", 0,
                getInt("json"));

        button.click();

        Assert.assertEquals("Count (id:count) should be 1", 1, getInt("count"));
        Assert.assertEquals("Count (id:json) should be 1", 1, getInt("json"));

        button.click();

        Assert.assertEquals("Count (id:count) should be 2", 2, getInt("count"));
        Assert.assertEquals("Count (id:json) should be 2", 2, getInt("json"));
    }

    private int getInt(String id) {
        return Integer.parseInt(byId(id).getText());
    }
}
