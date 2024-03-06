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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class MainIT extends ChromeDeviceTest {
    @Before
    public void init() {
        open();
        waitForServiceWorkerReady();
        getDevTools().setCacheDisabled(true);
    }

    @Test
    public void appShellIsLoaded() {
        Assert.assertNotNull("Should load the app shell",
                findElement(By.id("outlet")));
    }

    @Test
    public void setOfflineAndReload_customOfflinePageIsLoaded() {
        setOfflineAndReload();

        Assert.assertNotNull("Should load the custom offline.html",
                findElement(By.id("offline")));
    }

    private void setOfflineAndReload() {
        getDevTools().setOfflineEnabled(true);
        executeScript("window.location.reload()");
        waitUntil(webDriver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }
}
