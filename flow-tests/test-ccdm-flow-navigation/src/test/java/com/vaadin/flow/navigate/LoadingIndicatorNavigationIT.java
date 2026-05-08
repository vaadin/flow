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
package com.vaadin.flow.navigate;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.function.Supplier;

@NotThreadSafe
public class LoadingIndicatorNavigationIT extends ChromeBrowserTest {

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();
        getDriver().get(getRootURL() + "/loading-indicator-navigation");
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
        waitForElementWithId(LoadingIndicatorNavigationView.SOURCE_LABEL_ID);
    }

    @Test
    public void slowNavigationShowsLoadingThroughout() throws InterruptedException {
        expectConnectionState("connected");

        testBench().disableWaitForVaadin();
        // Navigate to slow target view
        findElement(
                By.id(LoadingIndicatorNavigationView.SLOW_NAVIGATE_BUTTON_ID))
                .click();

        // Wait for navigation to complete
        waitForElementWithId(LoadingIndicatorNavigationSlowTargetView.TARGET_LABEL_ID);
        Thread.sleep(100); // NOSONAR client request processing

        // State should be "loading" from the follow-up data loading request
        expectConnectionState("loading");

        // Wait for loading to complete
        waitForElementWithId(LoadingIndicatorNavigationSlowTargetView.DATA_LABEL_ID);
    }

    @Test
    public void fastNavigationHandledCorrectly() {
        expectConnectionState("connected");

        findElement(
                By.id(LoadingIndicatorNavigationView.FAST_NAVIGATE_BUTTON_ID))
                .click();

        // Wait for navigation and data loading follow-up request
        waitForElementWithId(LoadingIndicatorNavigationFastTargetView.DATA_LABEL_ID);

        // State should be "connected" after fast navigation
        expectConnectionState("connected");
    }

    /**
     * Checks that the connection state matches the expected value by polling
     * the JavaScript window.Vaadin.connectionState.state property.
     */
    private void expectConnectionState(String state) {
        Assert.assertEquals(state, executeScript(
                        "return window.Vaadin.connectionState.state;"));
    }

    private void waitForElementWithId(String id) {
        // Wait for navigation to complete
        waitUntil(driver -> {
            Assert.assertNotNull(driver);
            Object result = ((JavascriptExecutor) driver).executeScript("return document.getElementById(arguments[0]) !== null;", id);
            return result != null;
        }, 1);
    }

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context-path";
    }
}
