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
package com.vaadin.flow.navigate;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeDeviceTest;
import com.vaadin.testbench.TestBenchElement;

import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ObjectNode;

import static com.vaadin.flow.navigate.HelloWorldView.NAVIGATE_ABOUT;

public class ServiceWorkerIT extends ChromeDeviceTest {

    @Test
    public void onlineRoot_serviceWorkerInstalled_serviceWorkerActive() {
        getDriver().get(getRootURL() + "/");
        waitForDevServer();
        waitForServiceWorkerReady();

        boolean serviceWorkerActive = (boolean) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        "const resolve = arguments[arguments.length - 1];"
                                + "navigator.serviceWorker.ready.then( function(reg) { resolve(!!reg.active); });");
        Assert.assertTrue("service worker not installed", serviceWorkerActive);
    }

    @Test
    public void offlineRoot_reload_viewReloaded() {
        openPageAndPreCacheWhenDevelopmentMode("/");

        // Confirm that app shell is loaded
        Assert.assertNotNull("Should have outlet when loaded online",
                findElement(By.id("outlet")));

        // Confirm that client side view is loaded
        Assert.assertNotNull(
                "Should have <about-view> in DOM when loaded online",
                findElement(By.tagName("about-view")));

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

            // Confirm that app shell is loaded
            Assert.assertNotNull("Should have outlet when loaded offline",
                    findElement(By.id("outlet")));

            // Confirm that client side view is loaded
            Assert.assertNotNull(
                    "Should have <about-view> in DOM when loaded offline",
                    findElement(By.tagName("about-view")));
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineNonRoot_reload_viewReloaded() {
        Runnable navigate = () -> {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div")
                    .id("another-content").isDisplayed());
        };
        openPageAndPreCacheWhenDevelopmentMode("/", navigate);

        // Set offline network conditions in ChromeDriver
        getDevTools().setOfflineEnabled(true);

        try {
            navigate.run();
            // Reload the page in offline mode
            executeScript("window.location.reload();");
            waitUntil(webDriver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            TestBenchElement anotherView = $("another-view").first();
            Assert.assertTrue(
                    anotherView.$("*").id("another-content").isDisplayed());
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineDeepPath_reload_viewReloaded_baseUrlRewritten() {
        Runnable navigate = () -> {
            $("main-view").first().$("a").id("menu-deep-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div")
                    .id("another-content").isDisplayed());
        };
        openPageAndPreCacheWhenDevelopmentMode("/", navigate);

        // Set offline network conditions in ChromeDriver
        getDevTools().setOfflineEnabled(true);

        try {
            navigate.run();

            // Reload the page in offline mode
            executeScript("window.location.reload();");
            waitUntil(webDriver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            TestBenchElement anotherView = waitUntil(
                    driver -> $("another-view").first());
            Assert.assertTrue(
                    anotherView.$("*").id("another-content").isDisplayed());

            // Verify <base href> by navigating with a relative link
            $("main-view").first().$("a").id("menu-about").click();

            WebElement aboutView = waitUntil(
                    driver -> findElement(By.tagName("about-view")));
            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/about"));
            Assert.assertNotNull("Should have <about-view> in DOM", aboutView);
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineTsView_navigateToOtherTsView_navigationSuccessful() {
        Runnable navigate = () -> {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div")
                    .id("another-content").isDisplayed());
        };
        openPageAndPreCacheWhenDevelopmentMode("/about", navigate);

        MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/about"));

        // Set offline network conditions in ChromeDriver
        getDevTools().setOfflineEnabled(true);

        try {

            navigate.run();
            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            TestBenchElement anotherView = $("another-view").first();
            Assert.assertTrue(
                    anotherView.$("*").id("another-content").isDisplayed());
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineServerView_navigateToTsView_navigationSuccessful() {
        Runnable navigate = () -> {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div")
                    .id("another-content").isDisplayed());
        };
        openPageAndPreCacheWhenDevelopmentMode("/hello", navigate);

        // Set offline network conditions in ChromeDriver
        getDevTools().setOfflineEnabled(true);

        try {
            navigate.run();

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            TestBenchElement anotherView = $("another-view").first();
            Assert.assertTrue(
                    anotherView.$("*").id("another-content").isDisplayed());
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineTsView_navigateToServerView_offlineStubShown() {
        openPageAndPreCacheWhenDevelopmentMode("/another");

        getDevTools().setOfflineEnabled(true);
        try {
            $("main-view").first().$("a").id("menu-hello").click();

            waitForElementPresent(By.tagName("iframe"));
            WebElement offlineStub = findElement(By.tagName("iframe"));
            driver.switchTo().frame(offlineStub);
            Assert.assertNotNull(findElement(By.className("offline")));
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineServerView_navigateToServerView_offlineStubShown() {
        openPageAndPreCacheWhenDevelopmentMode("/");

        getDevTools().setOfflineEnabled(true);
        try {
            waitUntil(driver -> $("main-view").first().$("a").id("menu-hello"))
                    .click();

            waitForElementPresent(By.tagName("iframe"));
            WebElement offlineStub = findElement(By.tagName("iframe"));
            driver.switchTo().frame(offlineStub);
            Assert.assertNotNull(findElement(By.className("offline")));
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offlineStub_backOnline_stubRemoved_serverViewShown() {
        openPageAndPreCacheWhenDevelopmentMode("/");
        getDevTools().setOfflineEnabled(true);

        try {
            $("main-view").first().$("a").id("menu-hello").click();
            waitForElementPresent(By.tagName("iframe"));

            getDevTools().setOfflineEnabled(false);

            waitForElementNotPresent(By.tagName("iframe"));
            Assert.assertNotNull(findElement(By.id(NAVIGATE_ABOUT)));
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offline_cachedResourceRequest_resourceLoaded() {
        getDriver().get(getRootURL());
        waitForDevServer();
        waitForServiceWorkerReady();
        getDevTools().setOfflineEnabled(true);
        try {
            // verify that we can retrieve a cached file (manifest.webmanifest)
            driver.get(getRootURL() + "/manifest.webmanifest");

            // expect JSON contents wrapped in <pre>
            String jsonString = findElement(By.tagName("pre")).getText();
            ObjectNode json = JacksonUtils.readTree(jsonString);
            Assert.assertTrue(json.has("name"));
            Assert.assertTrue(json.has("short_name"));
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    private void openPageAndPreCacheWhenDevelopmentMode(String targetView) {
        openPageAndPreCacheWhenDevelopmentMode(targetView, () -> {
        });
    }

    private void openPageAndPreCacheWhenDevelopmentMode(String targetView,
            Runnable activateViews) {
        getDriver().get(getRootURL() + targetView);
        waitForDevServer();
        waitForServiceWorkerReady();

        boolean isDevMode = Boolean.getBoolean("vaadin.test.developmentMode");
        if (isDevMode) {
            // In production mode all views are supposed to be already in the
            // bundle, but in dev mode they are loaded at runtime
            // So, for dev mode, pre cache required views
            activateViews.run();

            // In addition not all external resources are cached when the page
            // opens
            // first time, so we need to reload the page even if there is no
            // navigation to other views
            getDriver().get(getRootURL() + targetView);
            waitForDevServer();
            waitForServiceWorkerReady();
        }
    }

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context-path";
    }
}
