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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BasicViewsIT extends ChromeBrowserTest {

    @Test
    public void rootViewShown() throws Exception {
        getDriver().get(getRootURL() + "/");
        Assert.assertEquals("This is the root view",
                $("*").id("view").getText());
    }

    @Test
    public void helloViewShown() throws Exception {
        getDriver().get(getRootURL() + "/hello");
        Assert.assertEquals("This is the Hello view",
                $("*").id("view").getText());
    }

    @Test
    public void invalidViewShowsNotFound() throws Exception {
        getDriver().get(getRootURL() + "/nonexistant");

        Assert.assertTrue(getDriver().getPageSource()
                .contains("Could not navigate to 'nonexistant'"));
    }

}
