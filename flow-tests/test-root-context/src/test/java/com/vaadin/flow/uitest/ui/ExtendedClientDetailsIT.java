/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ExtendedClientDetailsIT extends ChromeBrowserTest {

    @Test
    public void testExtendedClientDetails_availableImmediately() {
        open();

        // Values should be available immediately without clicking any button
        verifyDimensionGreaterThan50("sh", "window.screen.height");
        verifyDimensionGreaterThan50("sw", "window.screen.width");
        verifyDimensionGreaterThan50("wh", "window.innerHeight");
        verifyDimensionGreaterThan50("ww", "window.innerWidth");
        verifyDimensionGreaterThan50("bh", "document.body.clientHeight");
        verifyDimensionGreaterThan50("bw", "document.body.clientWidth");
        try {
            Double.parseDouble($(TestBenchElement.class).id("pr").getText());
        } catch (NumberFormatException nfe) {
            Assert.fail("Could not parse reported device pixel ratio");
        }
        Assert.assertTrue("false".equalsIgnoreCase(
                $(TestBenchElement.class).id("td").getText()));
    }

    @Test
    public void testExtendedClientDetails_reportsSomething() {
        open();

        $(TestBenchElement.class).id("fetch-values").click();

        verifyDimensionGreaterThan50("sh", "window.screen.height");
        verifyDimensionGreaterThan50("sw", "window.screen.width");
        verifyDimensionGreaterThan50("wh", "window.innerHeight");
        verifyDimensionGreaterThan50("ww", "window.innerWidth");
        verifyDimensionGreaterThan50("bh", "document.body.clientHeight");
        verifyDimensionGreaterThan50("bw", "document.body.clientWidth");
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

    private void verifyDimensionGreaterThan50(String elementId,
            String jsExecution) {
        String elementText = $(TestBenchElement.class).id(elementId).getText();
        Object executionResult = getCommandExecutor()
                .executeScript(("return " + jsExecution + ";"));

        try {
            int reportedValue = Integer.parseInt(elementText);
            int jsValue = ((Number) executionResult).intValue();

            Assert.assertTrue(
                    "reported value for " + elementId
                            + " should be > 50, but was: " + reportedValue,
                    reportedValue > 50);
            Assert.assertTrue(
                    "js execution value for " + elementId
                            + " should be > 50, but was: " + jsValue,
                    jsValue > 50);
        } catch (NumberFormatException nfe) {
            Assert.fail("Could not parse dimension value for " + elementId);
        }
    }
}
