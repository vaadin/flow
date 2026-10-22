/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

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

        assertBackNavigatesThroughEachUrl();
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

        assertBackNavigatesThroughEachUrl();
    }

    @Test
    public void reload_preservedViewWithQueryParams_historyIsNotChanged() {
        open();
        $(TestBenchElement.class).id("link-one").click();
        waitForUrlEndingWith("?param=one");
        long historyLength = getHistoryLength();

        driver.navigate().refresh();
        waitForElementPresent(By.id("link-one"));

        Assert.assertEquals("Reload should not add a history entry",
                historyLength, getHistoryLength());
        driver.navigate().back();
        waitForUrlEndingWith(PreserveOnRefreshNavigationView.VIEW_PATH);
    }

    private long getHistoryLength() {
        return (Long) executeScript("return window.history.length");
    }

    private void assertBackNavigatesThroughEachUrl() {
        driver.navigate().back();
        waitForUrlEndingWith("?param=two");
        driver.navigate().back();
        waitForUrlEndingWith("?param=one");
        driver.navigate().back();
        waitForUrlEndingWith(PreserveOnRefreshNavigationView.VIEW_PATH);
        driver.navigate().forward();
        waitForUrlEndingWith("?param=one");
    }

    private void waitForUrlEndingWith(String expectedSuffix) {
        try {
            waitUntil(d -> d.getCurrentUrl().endsWith(expectedSuffix));
        } catch (TimeoutException e) {
            Assert.fail("Expected URL ending with '" + expectedSuffix
                    + "' but was '" + driver.getCurrentUrl() + "'");
        }
    }

}
