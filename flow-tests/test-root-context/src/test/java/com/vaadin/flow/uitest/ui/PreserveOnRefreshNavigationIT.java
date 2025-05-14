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

public class PreserveOnRefreshNavigationIT extends ChromeBrowserTest {

    @Test
    public void routerLink_selfNavigationWithQueryParams_urlChanges() {
        open();
        $(TestBenchElement.class).id("link-one").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=one"));
        $(TestBenchElement.class).id("link-two").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=two"));
        $(TestBenchElement.class).id("link-three").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=three"));
    }

    @Test
    public void programmaticNavigation_selfNavigationWithQueryParams_urlChanges() {
        open();
        $(TestBenchElement.class).id("button-one").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=one"));
        $(TestBenchElement.class).id("button-two").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=two"));
        $(TestBenchElement.class).id("button-three").click();
        Assert.assertTrue(driver.getCurrentUrl().contains("?param=three"));
    }

}
