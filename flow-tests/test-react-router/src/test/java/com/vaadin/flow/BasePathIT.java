/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BasePathIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/view/view/com.vaadin.flow.BasePathView";
    }

    @Test
    public void navigationTo_routeWithBasePath_succeeds() {
        open();

        waitForDevServer();

        Assert.assertTrue(
                $(SpanElement.class).withText("BasePathView").exists());
    }

}
