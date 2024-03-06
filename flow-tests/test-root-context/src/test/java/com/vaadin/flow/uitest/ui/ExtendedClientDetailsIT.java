/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;

public class ExtendedClientDetailsIT extends ChromeBrowserTest {

    @Test
    public void testExtendedClientDetails_reportsSomething() {
        open();

        $(TestBenchElement.class).id("fetch-values").click();

        verifyTextMatchesJSExecution("sh", "window.screen.height");
        verifyTextMatchesJSExecution("sw", "window.screen.width");
        verifyTextMatchesJSExecution("wh", "window.innerHeight");
        verifyTextMatchesJSExecution("ww", "window.innerWidth");
        verifyTextMatchesJSExecution("bh", "document.body.clientHeight");
        verifyTextMatchesJSExecution("bw", "document.body.clientWidth");
        try {
            Double.parseDouble($(TestBenchElement.class).id("pr").getText());
        } catch (NumberFormatException nfe) {
            Assert.fail("Could not parse reported device pixel ratio");
        }
        Assert.assertTrue("false".equalsIgnoreCase(
                $(TestBenchElement.class).id("td").getText()));

    }

    @Test
    public void testExtendedClientDetails_predefinedDevicePixelRatioTouchSupport_reportedCorrectly() {
        open();

        $(TestBenchElement.class).id("set-values").click();
        $(TestBenchElement.class).id("fetch-values").click();

        try {
            double pixelRatio = Double
                    .parseDouble($(TestBenchElement.class).id("pr").getText());
            Assert.assertEquals("Invalid Pixel ratio reported", 2.0D,
                    pixelRatio, 0.1D);
        } catch (NumberFormatException nfe) {
            Assert.fail("Could not parse reported device pixel ratio");
        }
        Assert.assertTrue("true".equalsIgnoreCase(
                $(TestBenchElement.class).id("td").getText()));

    }

    private void verifyTextMatchesJSExecution(String elementId,
            String jsExecution) {
        String elementText = $(TestBenchElement.class).id(elementId).getText();
        Object executionResult = getCommandExecutor()
                .executeScript(("return " + jsExecution + ";"));
        Assert.assertEquals(
                "reported value did not match js execution for " + elementId,
                executionResult.toString(), elementText);
    }
}
