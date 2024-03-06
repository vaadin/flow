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
