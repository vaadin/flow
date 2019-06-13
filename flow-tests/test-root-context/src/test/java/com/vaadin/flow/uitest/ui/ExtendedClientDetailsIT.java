/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;

public class ExtendedClientDetailsIT extends ChromeBrowserTest {

    @Test
    public void testExtendedClientDetails_reportsSomething() {
        open();

        $(TestBenchElement.class).id("fetch-values").click();

        verifyHasNumber("sw");
        verifyHasNumber("sh");
        verifyHasNumber("bw");
        verifyHasNumber("bh");
        verifyHasNumber("ww");
        verifyHasNumber("wh");
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

    private void verifyHasNumber(String elementId) {
        String elementText = $(TestBenchElement.class).id(elementId).getText();
        try {
            Integer.parseInt(elementText);
        } catch (NumberFormatException nfe) {
            Assert.fail(
                    "Could not parse text <" + elementText + "> as number from "
                            + "element " + "with ID <" + elementId + ">");
        }
    }
}
