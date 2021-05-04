/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.pwatest.ui;

import java.io.*;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.mobile.NetworkConnection;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class PwaTestIT extends ChromeDeviceTest {

    @Test
    public void testPwaOfflinePath() throws IOException {
        open();
        waitForServiceWorkerReady();

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

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
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }
}
