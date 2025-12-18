/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class MainIT extends ChromeDeviceTest {

    // Ping actual existing file and not no route page, which now returns 404
    final String VITE_PING_PATH = "/VAADIN/generated/";

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
                sendVitePingRequest(VITE_PING_PATH + "vaadin.ts"));

        getDevTools().setOfflineEnabled(true);

        // Different file to not get cached.
        Assert.assertFalse("Should reject Vite ping requests when offline",
                sendVitePingRequest(VITE_PING_PATH + "theme.js"));
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

    private Boolean sendVitePingRequest(String VITE_PING_PATH) {
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
