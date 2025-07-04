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
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import org.junit.Before;

import org.openqa.selenium.By;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.flow.testutil.ChromeDeviceTest;

public class BasicComponentIT extends ChromeDeviceTest {

    @Before
    public void init() {
        getDriver().get(getRootURL());
        waitForDevServer();
        getDriver().get(getRootURL() + "/basic-component.html");
    }

    @Test
    public void componentIsLoaded() {
        checkLogsForErrors();

        TestBenchElement component = $("basic-component").first();
        TestBenchElement h1 = component.$("h1").first();
        Assert.assertEquals("Basic Component", h1.getText());
    }

    @Test
    public void cssIsAppliedToComponent_butNotTotemplate() {
        checkLogsForErrors();

        TestBenchElement component = $("basic-component").first();
        TestBenchElement h1 = component.$("h1").first();
        Assert.assertEquals("rgba(211, 211, 211, 1)", h1.getCssValue("color"));

        Assert.assertEquals(
                "Exported webcomponent h1 font-family should come from styles.css",
                "Ostrich", h1.getCssValue("font-family"));

        TestBenchElement htmlH1 = $("h1").first();
        Assert.assertEquals("H1 outside component should not have color.",
                "rgba(0, 0, 0, 1)", htmlH1.getCssValue("color"));

        Assert.assertFalse(
                "body h1 font-family should not come from styles.css",
                "Ostrich".equals(htmlH1.getCssValue("font-family")));
    }
}
