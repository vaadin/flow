/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.eagerbootstrap;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ParameterIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/parameter";
    }

    private void openWithParameter(String parameter) {
        String url = getTestURL();
        getDriver().get(url + "/" + parameter);
        waitForDevServer();
    }

    @Test
    public void setParameterCalledAsExpected() {
        openWithParameter("foo");
        Assert.assertEquals("setParameter called with: foo",
                getParametersText());
        $("*").id("barLink").click();
        Assert.assertEquals(
                "setParameter called with: foo\nsetParameter called with: bar",
                getParametersText());
    }

    private String getParametersText() {
        return $("*").id("parameters").getText();
    }

    private int getInstance() {
        String instanceText = $("*").id("instance").getText();
        return Integer
                .parseInt(instanceText.replace("This is view instance ", ""));
    }

}
