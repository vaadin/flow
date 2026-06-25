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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

/**
 * Guards against vaadin/flow#24800: after a frontend bundle rebuild an
 * installed PWA showed a permanent blank screen because the service worker
 * shipped by the rebuild called {@code self.skipWaiting()} and therefore
 * activated while pages from the previous build were still open. It took those
 * pages over mid-session and then 404'd the old-hash bundle they still
 * reference - the previous build's files are gone from both the new precache
 * and the redeployed server.
 * <p>
 * Reproducing the 404 end to end would need two frontend builds with different
 * bundle hashes, which a single-build IT cannot produce, so this test instead
 * asserts the actual fix on the real generated service worker: it must not call
 * {@code self.skipWaiting()}. Without that call a worker built by a new
 * frontend build stays in "waiting" until the previous build's pages close,
 * handing over control cleanly between page loads instead of mid-load.
 */
public class ServiceWorkerRebuildIT extends ChromeDeviceTest {

    @Override
    protected String getTestPath() {
        return "";
    }

    @Test
    public void generatedServiceWorkerDoesNotSkipWaiting() {
        open();
        waitForServiceWorkerReady();

        String serviceWorker = serviceWorkerSource();

        Assert.assertTrue(
                "Sanity check: sw.js was not the generated service worker",
                serviceWorker.contains("addEventListener"));

        // Matching the bare token "skipWaiting" is not enough: bundled workbox
        // code mentions it in a doc comment. The actual call is the signal.
        Assert.assertFalse(
                "Issue #24800: the generated service worker must not call "
                        + "self.skipWaiting(). Otherwise a service worker built "
                        + "by a new frontend build activates while pages from "
                        + "the previous build are still open, takes them over "
                        + "mid-session and 404s the old-hash bundle they "
                        + "reference, leaving a permanent blank screen.",
                serviceWorker.contains("self.skipWaiting"));
    }

    private String serviceWorkerSource() {
        return (String) ((JavascriptExecutor) getDriver()).executeAsyncScript(
                "const done = arguments[arguments.length - 1];"
                        + "fetch('sw.js').then(r => r.text()).then(done)"
                        + ".catch(() => done(''));");
    }
}
