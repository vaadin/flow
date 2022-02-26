/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import io.github.bonigarcia.wdm.WebDriverManager;

import org.junit.Before;
import org.junit.BeforeClass;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class MyComponentIT extends ChromeBrowserTest {
    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Before
    public void init() {
        getDriver().get(getRootURL() + "/my-component.html");
        waitForDevServer();
    }

    @Test
    public void componentIsLoaded() {
        checkLogsForErrors();

        TestBenchElement myComponent = $("my-component").first();
        TestBenchElement h1 = myComponent.$("h1").first();
        Assert.assertEquals("My Component", h1.getText());
    }

    @Test
    public void themeIsAppliedToComponent() {
        checkLogsForErrors();

        TestBenchElement myComponent = $("my-component").first();
        TestBenchElement h1 = myComponent.$("h1").first();
        Assert.assertEquals("rgba(255, 0, 0, 1)", h1.getCssValue("color"));
    }
}
