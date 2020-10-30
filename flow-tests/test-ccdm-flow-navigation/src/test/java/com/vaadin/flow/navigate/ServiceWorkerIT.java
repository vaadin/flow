/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.mobile.NetworkConnection;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class ServiceWorkerIT extends ChromeDeviceTest {

    @Test
    public void testOfflineStart() throws IOException {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();

        // Confirm that app shell is loaded
        Assert.assertNotNull("Should have outlet when loaded online",
                findElement(By.id("outlet")));

        // Confirm that client side view is loaded
        Assert.assertNotNull("Should have <about-view> in DOM when loaded online",
                findElement(By.tagName("about-view")));

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

            // Confirm that app shell is loaded
            Assert.assertNotNull("Should have outlet when loaded offline",
                    findElement(By.id("outlet")));

            // Confirm that client side view is loaded
            Assert.assertNotNull("Should have <about-view> in DOM when loaded offline",
                    findElement(By.tagName("about-view")));
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void openTsView_goOffline_navigateToOtherTsView_navigationSuccessful() throws IOException {
        getDriver().get(getRootURL() + "/about");
        waitForServiceWorkerReady();

        MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/about"));

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

        try {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div").id("another-content")
                    .isDisplayed());

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            Assert.assertTrue(getInShadowRoot(findElement(By.tagName("another-view")),
                    By.id("another-content")).isDisplayed());
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

}
