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
 * Reproduces vaadin/flow#24800: after a frontend bundle rebuild an installed
 * PWA shows a permanent blank screen because the service worker shipped by the
 * rebuild takes over an already-open page mid-session (skipWaiting +
 * clientsClaim) and then 404s the old-hash bundle the loaded page still
 * references.
 * <p>
 * A faithful reproduction needs two frontend builds with different bundle
 * hashes, which a single-build IT cannot produce. The test therefore drives a
 * model service worker ({@code swrebuild/sw-model.js}) that mirrors the policy
 * of Flow's generated {@code sw.ts}. Whether a freshly installed worker takes
 * over mid-session is read from the real generated {@code sw.js} (specifically
 * whether it calls {@code self.skipWaiting()}), so this test turns green
 * automatically once the platform no longer takes over mid-session.
 * <p>
 * The assertion is made on the still-open page (no navigation), so the result
 * is deterministic: with the current takeover policy the rebuilt worker claims
 * the page and the bundle it depends on 404s; without it the page keeps its
 * original worker and the bundle stays reachable.
 */
public class ServiceWorkerRebuildIT extends ChromeDeviceTest {

    @Override
    protected String getTestPath() {
        return "";
    }

    @Test
    public void installedPwaRecoversAfterFrontendRebuild() {
        boolean takeover = generatedServiceWorkerTakesOverMidSession();

        cleanSlate();

        // An installed PWA: OLD build, controlled by its service worker.
        installBaseline(takeover);
        Assert.assertEquals("Baseline: the installed PWA should render",
                "APP RENDERED (build OLD)", appText());

        // A frontend rebuild ships a new service worker (mirroring the real
        // sw.ts takeover policy).
        registerRebuild(takeover);

        // The running PWA must still be able to load the bundle its loaded page
        // references. With the current sw.ts policy the rebuilt worker takes
        // over mid-session and this 404s -> the blank screen of #24800.
        Assert.assertEquals(
                "Issue #24800: after a frontend rebuild the running PWA must "
                        + "still reach the bundle the loaded page depends on; "
                        + "the rebuilt service worker must not take over "
                        + "mid-session and 404 it",
                200L, fetchStatus("bundle-old.js"));
    }

    /**
     * Reads the real generated service worker to determine whether a freshly
     * installed worker takes over an already-open page. The signal is the
     * {@code self.skipWaiting()} call - matching the bare token
     * {@code skipWaiting} is not enough because bundled workbox code mentions
     * it in a doc comment.
     */
    private boolean generatedServiceWorkerTakesOverMidSession() {
        getDriver().get(getRootURL() + "/");
        String sw = (String) executeAsync(
                "const done = arguments[arguments.length - 1];"
                        + "fetch('sw.js').then(r => r.text()).then(done)"
                        + ".catch(() => done(''));");
        return sw.contains("self.skipWaiting");
    }

    private void cleanSlate() {
        getDriver().get(getRootURL() + "/swrebuild/shell-old.html");
        executeAsync("const done = arguments[arguments.length - 1];"
                + "(async () => {"
                + "  for (const r of await navigator.serviceWorker.getRegistrations()) await r.unregister();"
                + "  for (const k of await caches.keys()) await caches.delete(k);"
                + "  done(true);" + "})();");
    }

    /**
     * Installs the OLD build of the model worker and reloads so the page is
     * served through it (control on reload does not depend on clientsClaim).
     */
    private void installBaseline(boolean takeover) {
        Boolean ready = (Boolean) executeAsync(
                "const q = arguments[0];"
                        + "const done = arguments[arguments.length - 1];"
                        + "navigator.serviceWorker.register('sw-model.js' + q)"
                        + "  .then(() => navigator.serviceWorker.ready)"
                        + "  .then(() => done(true)).catch(() => done(false));",
                "?build=OLD&takeover=" + takeover);
        Assert.assertTrue("Baseline service worker should install", ready);
        getDriver().get(getRootURL() + "/swrebuild/shell-old.html");
        Assert.assertTrue("Page should be controlled by the service worker",
                (Boolean) executeScript(
                        "return !!navigator.serviceWorker.controller;"));
    }

    /**
     * Registers the NEW build of the model worker and waits until any
     * mid-session takeover has settled, so the following assertion is made
     * against the page's final controller.
     */
    private void registerRebuild(boolean takeover) {
        executeAsync("const q = arguments[0];" + "const want = arguments[1];"
                + "const done = arguments[arguments.length - 1];"
                + "(async () => {"
                + "  await navigator.serviceWorker.register('sw-model.js' + q);"
                + "  for (let i = 0; i < 30; i++) {"
                + "    const c = navigator.serviceWorker.controller;"
                + "    if (want && c && c.scriptURL.includes('build=NEW')) break;"
                + "    await new Promise(r => setTimeout(r, 50));" + "  }"
                + "  done(true);" + "})();", "?build=NEW&takeover=" + takeover,
                takeover);
    }

    private String appText() {
        return (String) executeScript(
                "return document.getElementById('app').textContent;");
    }

    private long fetchStatus(String url) {
        Number status = (Number) executeAsync("const u = arguments[0];"
                + "const done = arguments[arguments.length - 1];"
                + "fetch(u).then(r => done(r.status)).catch(() => done(-1));",
                url);
        return status.longValue();
    }

    private Object executeAsync(String script, Object... args) {
        return ((JavascriptExecutor) getDriver()).executeAsyncScript(script,
                args);
    }
}
