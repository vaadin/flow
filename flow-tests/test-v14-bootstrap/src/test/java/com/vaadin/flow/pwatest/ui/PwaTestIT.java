/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.pwatest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class PwaTestIT extends ChromeDeviceTest {

    @Test
    public void testPwaOfflinePath() {
        open();
        waitForServiceWorkerReady();

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

            // Assert page title
            waitForElementPresent(By.tagName("head"));
            WebElement head = findElement(By.tagName("head"));
            waitForElementPresent(By.tagName("title"));
            WebElement title = head.findElement(By.tagName("title"));
            Assert.assertEquals(ParentLayout.PWA_NAME,
                    executeScript("return arguments[0].textContent", title));
            Assert.assertEquals(ParentLayout.PWA_NAME,
                    executeScript("return document.title;"));

            // Assert default offline.html page contents
            WebElement body = findElement(By.tagName("body"));
            Assert.assertTrue("Should not have outlet when loaded offline",
                    body.findElements(By.id("outlet")).isEmpty());

            WebElement offline = body.findElement(By.id("offline"));
            Assert.assertEquals(ParentLayout.PWA_NAME,
                    offline.findElement(By.tagName("h1")).getText());
            WebElement message = offline.findElement(By.className("message"));
            Assert.assertTrue("Should have “offline” in message",
                    message.getText().toLowerCase().contains("offline"));
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }
}
