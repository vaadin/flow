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
import org.junit.Before;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class MainIT extends ChromeDeviceTest {
    @Before
    public void init() {
        getDevTools().setCacheDisabled(true);
    }

    @Test
    public void openHomePage_pageIsLoaded() {
        openPage("/");

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "Home Page");

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));
    }

    @Test
    public void openHomePage_setOfflineAndReload_pageIsLoadedFromCache() {
        openPage("/");
        setOfflineAndReload();

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "Home Page");

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));
    }

    @Test
    public void openAboutPage_pageIsLoaded() {
        openPage("/about");

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "About Page");

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));
    }

    @Test
    public void openAboutPage_setOfflineAndReload_pageIsLoadedFromCache() {
        openPage("/about");
        setOfflineAndReload();

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "About Page");

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));
    }

    private Boolean isAppThemeLoaded() {
        return (Boolean) executeScript(
                "return window.Vaadin.theme !== undefined && "
                        + "window.Vaadin.theme.injectedGlobalCss.length !== 0");
    }

    private void setOfflineAndReload() {
        getDevTools().setOfflineEnabled(true);
        executeScript("window.location.reload()");
        waitUntil(webDriver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }

    private void openPage(String url) {
        getDriver().get(getRootURL() + url);
        try {
            waitForServiceWorkerReady();
        } finally {
            checkLogsForErrors();
        }
    }
}
