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
    final String VITE_PING_PATH = "/VAADIN";

    @Before
    public void init() {
        getDevTools().setCacheDisabled(true);
    }

    @Test
    public void openHomePage_pageIsLoaded() {
        openPage("/");

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "Home Page");

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());
    }

    @Test
    public void openHomePage_setOffline_vitePingRequestIsRejected() {
        openPage("/");

        Assert.assertTrue("Should allow Vite ping requests when online",
                sendVitePingRequest());

        getDevTools().setOfflineEnabled(true);

        Assert.assertFalse("Should reject Vite ping requests when offline",
                sendVitePingRequest());
    }

    @Test
    public void openHomePage_setOfflineAndReload_pageIsLoadedFromCache() {
        openPage("/");
        getDevTools().setOfflineEnabled(true);
        reloadPage();

        checkLogsForErrors(msg -> msg.contains(VITE_PING_PATH)
                || !msg.contains("Failed to load"));

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "Home Page");

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());
    }

    @Test
    public void openAboutPage_pageIsLoaded() {
        openPage("/about");

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "About Page");

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());
    }

    @Test
    public void openAboutPage_setOfflineAndReload_pageIsLoadedFromCache() {
        openPage("/about");
        getDevTools().setOfflineEnabled(true);
        reloadPage();

        checkLogsForErrors(msg -> msg.contains(VITE_PING_PATH)
                || !msg.contains("Failed to load"));

        WebElement h1 = $("h1").first();
        Assert.assertEquals(h1.getText(), "About Page");

        Assert.assertTrue("Should load the app theme", isAppThemeLoaded());
    }

    private Boolean isAppThemeLoaded() {
        return (Boolean) executeScript(
                "return window.Vaadin.theme !== undefined && "
                        + "window.Vaadin.theme.injectedGlobalCss.length !== 0");
    }

    private Boolean sendVitePingRequest() {
        return (Boolean) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        "const done = arguments[arguments.length - 1];"
                                + "fetch(arguments[0])"
                                + "  .then((response) => done(response.ok))"
                                + "  .catch(() => done(false))",
                        VITE_PING_PATH);
    }

    private void reloadPage() {
        executeScript("window.location.reload()");
        waitUntil(webDriver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }

    private void openPage(String url) {
        getDriver().get(getRootURL() + url);
        waitForDevServer();
        waitForServiceWorkerReady();
        // Now that the SW is ready, reload the page
        // to fill the SW runtime cache with dev resources.
        reloadPage();
    }
}
