/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

/**
 * Tests what an open page gets from the service worker after the application
 * has been rebuilt.
 */
public class ServiceWorkerUpdateIT extends ChromeDeviceTest {

    private static final Pattern PRECACHED_BUNDLE_SCRIPT = Pattern
            .compile("VAADIN/build/[^\"'`]+\\.js");

    @Test
    public void precachedBundleFile_removedFromServer_servedFromPrecache()
            throws IOException {
        // Only a dev bundle has precached bundle files that the development
        // mode route could fetch from the network instead. With the Vite dev
        // server nothing is precached, and in production the files are always
        // served from the precache.
        Assume.assumeTrue("Not running with a dev bundle",
                Boolean.getBoolean("vaadin.test.devBundle"));
        open();
        waitForServiceWorkerControl();

        Matcher matcher = PRECACHED_BUNDLE_SCRIPT
                .matcher(readString(getRootURL() + "/sw.js"));
        Assert.assertTrue("The bundle scripts should be precached",
                matcher.find());
        String bundleScript = matcher.group();

        executeScript("document.cookie = arguments[0] + '=true'",
                RemovedBundleFilter.BUNDLE_REMOVED_COOKIE);
        try {
            Assert.assertEquals(
                    "The server should no longer have the bundle script", 404,
                    getStatusWithoutServiceWorker(bundleScript));
            Assert.assertEquals(
                    "The bundle script should be served from the precache",
                    200L, fetchStatus(bundleScript));
        } finally {
            executeScript("document.cookie = arguments[0] + '=; max-age=0'",
                    RemovedBundleFilter.BUNDLE_REMOVED_COOKIE);
        }
    }

    @Test
    public void newServiceWorker_waitsUntilSkipWaitingRequested() {
        // The Vite dev server does not serve sw.js with a query string, which
        // the test uses to install a new worker
        Assume.assumeFalse("Running with the Vite dev server",
                Boolean.getBoolean("vaadin.test.viteDevServer"));
        open();
        waitForServiceWorkerControl();

        // Registering the worker script under another URL installs a new
        // worker, the same way as a changed sw.js after a rebuild
        Map<?, ?> result = (Map<?, ?>) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        """
                                const done = arguments[arguments.length - 1];
                                const stateReached = (worker, states) => new Promise((resolve) => {
                                  const check = () => states.includes(worker.state) && resolve();
                                  check();
                                  worker.addEventListener('statechange', check);
                                });
                                (async () => {
                                  const registration = await navigator.serviceWorker.getRegistration();
                                  const previous = registration.active;
                                  await navigator.serviceWorker.register(previous.scriptURL + '?update');
                                  const worker = registration.installing || registration.waiting || registration.active;
                                  await stateReached(worker, ['installed', 'activating', 'activated']);
                                  // Give a worker that skips waiting time to activate
                                  await new Promise((resolve) => setTimeout(resolve, 2000));
                                  if (registration.waiting !== worker || registration.active !== previous) {
                                    return { waiting: false };
                                  }
                                  const controllerChanged = new Promise((resolve) =>
                                    navigator.serviceWorker.addEventListener('controllerchange', resolve));
                                  // Same message as vite-devmode.ts sends after an update of
                                  // sw.ts, see the message listener in sw.ts
                                  worker.postMessage({ method: 'Vaadin.ServiceWorker.skipWaiting' });
                                  await stateReached(worker, ['activated']);
                                  await controllerChanged;
                                  return {
                                    waiting: true,
                                    controlling: navigator.serviceWorker.controller === worker
                                  };
                                })().then(done, (error) => done({ error: String(error) }));
                                """);

        Assert.assertNull("Updating the service worker failed",
                result.get("error"));
        Assert.assertEquals(
                "The new service worker should wait while the page is open",
                true, result.get("waiting"));
        Assert.assertEquals(
                "The new service worker should take over when asked to skip waiting",
                true, result.get("controlling"));
    }

    @Override
    protected String getTestPath() {
        return "";
    }

    private void waitForServiceWorkerControl() {
        waitForServiceWorkerReady();
        waitUntil(driver -> (Boolean) executeScript(
                "return !!navigator.serviceWorker.controller"));
    }

    private Object fetchStatus(String url) {
        return ((JavascriptExecutor) getDriver()).executeAsyncScript(
                "const done = arguments[arguments.length - 1];"
                        + "fetch(arguments[0]).then("
                        + "response => done(response.status), () => done(null));",
                url);
    }

    private int getStatusWithoutServiceWorker(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI
                .create(getRootURL() + "/" + url).toURL().openConnection();
        connection.setRequestProperty("Cookie",
                RemovedBundleFilter.BUNDLE_REMOVED_COOKIE + "=true");
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    private String readString(String url) throws IOException {
        try (InputStream stream = URI.create(url).toURL().openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
