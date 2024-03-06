/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.testutil.ChromeDeviceTest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

public class OfflineWithCustomContextIT extends ChromeDeviceTest {

    @Test
    // for https://github.com/vaadin/flow/issues/10177
    public void testPwaOfflinePath() {
        open();
        waitForServiceWorkerReady();

        // Confirm that app shell is loaded
        Assert.assertNotNull("Should have outlet when loaded online",
                findElement(By.id("outlet")));

        // Set offline network conditions in ChromeDriver
        getDevTools().setOfflineEnabled(true);

        try {
            Assert.assertEquals("navigator.onLine should be false", false,
                    executeScript("return navigator.onLine"));

            // Reload the page in offline mode
            executeScript("window.location.reload();");
            waitUntil(webDriver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

            // Assert custom offline.html page contents
            WebElement body = findElement(By.tagName("body"));
            Assert.assertTrue("Should not have outlet when loaded offline",
                    body.findElements(By.id("outlet")).isEmpty());

            WebElement offline = body.findElement(By.id("offline"));
            Assert.assertTrue(
                    "Should have the content defined in offline.html file",
                    offline.findElement(By.tagName("h1")).getText()
                            .contains("PWA With custom offline page"));
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Override
    protected String getTestPath() {
        return AbstractContextIT.JETTY_CONTEXT;
    }
}
