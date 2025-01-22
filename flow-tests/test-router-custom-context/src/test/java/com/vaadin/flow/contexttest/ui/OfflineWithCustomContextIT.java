/*
 * Copyright 2000-2025 Vaadin Ltd.
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
