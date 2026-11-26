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
package com.vaadin.flow.uitest.ui;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerDownloadIT extends ChromeBrowserTest {

    @Test
    public void clickUrl_invokesStartWithUrlAndNoFilename() {
        open();
        installDownloadShim();

        findElement(By.id("download-url")).click();

        Map<String, Object> recorded = waitForFirstDownload();
        Assert.assertEquals("/static/sample.bin", recorded.get("url"));
        Assert.assertNull(recorded.get("filename"));
    }

    @Test
    public void clickUrlWithFilename_invokesStartWithBothAndQuotesRoundTrip() {
        open();
        installDownloadShim();

        findElement(By.id("download-url-filename")).click();

        Map<String, Object> recorded = waitForFirstDownload();
        Assert.assertEquals("/static/sample.bin", recorded.get("url"));
        // The Java-side literal includes a quote; JSON encoding into the
        // emitted JS must preserve it byte-for-byte when it reaches the
        // browser.
        Assert.assertEquals(TriggerDownloadView.SUGGESTED_FILENAME,
                recorded.get("filename"));
    }

    @Test
    public void clickHandler_emitsRegisteredResourceUrl_andUrlServesHandlerBody() {
        open();
        installDownloadShim();

        findElement(By.id("download-handler")).click();

        Map<String, Object> recorded = waitForFirstDownload();
        String url = (String) recorded.get("url");
        // StreamResourceRegistry composes URIs under VAADIN/dynamic/resource.
        Assert.assertTrue("Expected a Vaadin dynamic-resource URL, got: " + url,
                url.contains("VAADIN/dynamic/resource"));

        // End-to-end check that Element.setAttribute(name, resource) actually
        // wired the URL to the DownloadHandler: fetching the recorded URL
        // from the browser should return the bytes the handler wrote.
        String body = fetchText(url);
        Assert.assertEquals(TriggerDownloadView.HANDLER_BODY, body);
    }

    @Test
    public void clickInput_resolvesUrlFromInputValueAtFireTime() {
        open();
        installDownloadShim();

        WebElement field = findElement(By.id("url-source"));
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = '/from-input/file.bin';", field);

        findElement(By.id("download-input")).click();

        Map<String, Object> recorded = waitForFirstDownload();
        Assert.assertEquals("/from-input/file.bin", recorded.get("url"));
    }

    @Test
    public void clickDownloadOnClick_urlServesBodyWithLazyFilename() {
        open();
        installDownloadShim();

        findElement(By.id("download-on-click")).click();

        String url = (String) waitForFirstDownload().get("url");
        Assert.assertEquals("200|attachment; filename=\""
                + TriggerDownloadView.LAZY_FILENAME + "\"|"
                + TriggerDownloadView.HANDLER_BODY, fetchResponse(url));
    }

    @Test
    public void clickDownloadOnClickFailure_urlRespondsWithServerError() {
        open();
        installDownloadShim();

        findElement(By.id("download-on-click-failure")).click();

        String url = (String) waitForFirstDownload().get("url");
        String response = fetchResponse(url);
        Assert.assertTrue("Expected a 500 response, got: " + response,
                response.startsWith("500|"));
    }

    // Replace window.Vaadin.Flow.download.start with a recorder so the IT
    // never actually triggers a save dialog or navigation. Each invocation
    // pushes {url, filename} onto window.__downloads.
    private void installDownloadShim() {
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__downloads = [];"
                        + "window.Vaadin.Flow.download.start = "
                        + "  (url, filename) => window.__downloads.push("
                        + "    {url: url, filename: filename ?? null});");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> waitForFirstDownload() {
        return (Map<String, Object>) waitUntil(d -> {
            Object value = ((JavascriptExecutor) d).executeScript(
                    "return (window.__downloads && window.__downloads.length)"
                            + " ? window.__downloads[0] : null;");
            return value;
        });
    }

    private String fetchText(String url) {
        // executeAsyncScript: last arg is the callback to invoke when done.
        return (String) ((JavascriptExecutor) getDriver())
                .executeAsyncScript("const url = arguments[0];"
                        + "const done = arguments[arguments.length - 1];"
                        + "fetch(url).then(r => r.text()).then(done)"
                        + "  .catch(e => done('FETCH_ERROR:' + e));", url);
    }

    // Fetches the URL and returns "status|content-disposition|body".
    private String fetchResponse(String url) {
        return (String) ((JavascriptExecutor) getDriver()).executeAsyncScript(
                """
                        const done = arguments[arguments.length - 1];
                        fetch(arguments[0])
                          .then(r => r.text().then(t => done(r.status + '|'
                              + r.headers.get('Content-Disposition') + '|' + t)))
                          .catch(e => done('FETCH_ERROR:' + e));
                        """,
                url);
    }

    @Test
    public void clickOpenTabUrl_callsWindowOpenWithBlankTargetAndDefaultFeatures() {
        open();
        installOpenShim();

        findElement(By.id("open-tab")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("https://example.com/docs", recorded.get("url"));
        Assert.assertEquals("_blank", recorded.get("target"));
        Assert.assertEquals("noopener,noreferrer", recorded.get("features"));
    }

    @Test
    public void clickOpenTabUrlWithFeatures_passesCustomFeaturesVerbatim() {
        open();
        installOpenShim();

        findElement(By.id("open-tab-features")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("https://example.com/help", recorded.get("url"));
        Assert.assertEquals("_blank", recorded.get("target"));
        // Custom features replace the defaults; the action passes whatever
        // the caller gave through to window.open verbatim.
        Assert.assertEquals(TriggerDownloadView.CUSTOM_FEATURES,
                recorded.get("features"));
    }

    @Test
    public void clickOpenTabInput_resolvesUrlFromInputValueAtFireTime() {
        open();
        installOpenShim();

        WebElement field = findElement(By.id("open-tab-url-source"));
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'https://example.com/from-input';",
                field);

        findElement(By.id("open-tab-input")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("https://example.com/from-input",
                recorded.get("url"));
        Assert.assertEquals("_blank", recorded.get("target"));
    }

    @Test
    public void clickOpenTabJavascriptInput_blocksWindowOpen() {
        open();
        installOpenShim();

        WebElement field = findElement(By.id("open-tab-url-source"));
        // Plain javascript: URL — the simplest variant. Leading
        // whitespace/case variants are covered by the unit test; here we
        // only need to prove the client guard prevents window.open from
        // being called at all.
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'javascript:window.__pwned = true';",
                field);

        findElement(By.id("open-tab-js-input")).click();

        // Click a safe URL afterwards so we have a positive signal that the
        // recording shim works end-to-end — without this we couldn't tell
        // "blocked correctly" from "click did nothing at all".
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'https://example.com/after-block';",
                field);
        findElement(By.id("open-tab-input")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals(
                "javascript: URL must not reach window.open — should be"
                        + " skipped, and only the subsequent safe click should"
                        + " be recorded",
                "https://example.com/after-block", recorded.get("url"));
        // Make sure the inline payload never ran in the page either.
        Object pwned = ((JavascriptExecutor) getDriver())
                .executeScript("return window.__pwned;");
        Assert.assertNull("javascript: URL must not execute in the opener",
                pwned);
    }

    @Test
    public void clickOpenTabHandler_opensRegisteredResourceServedInline() {
        open();
        installOpenShim();

        findElement(By.id("open-tab-handler")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("_blank", recorded.get("target"));
        Assert.assertEquals("noopener,noreferrer", recorded.get("features"));
        // Fetch the URL the new tab would load to check that the content is
        // served inline, which is what makes the browser show it in the tab.
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) ((JavascriptExecutor) getDriver())
                .executeAsyncScript("""
                        const done = arguments[arguments.length - 1];
                        fetch(arguments[0]).then(r => r.text().then(t => done({
                            disposition: r.headers.get('Content-Disposition'),
                            body: t})));
                        """, recorded.get("url"));
        Assert.assertTrue(
                "Expected inline content, got: " + response.get("disposition"),
                String.valueOf(response.get("disposition"))
                        .startsWith("inline"));
        Assert.assertEquals(TriggerDownloadView.NEW_TAB_CONTENT,
                response.get("body"));
    }

    // Replace window.open with a recorder so the IT does not depend on Chrome
    // actually opening a popup (headless Chrome's popup behaviour is fragile)
    // and so the popup blocker is irrelevant. Each invocation pushes
    // {url, target, features} onto window.__opens; the shim returns a stub
    // object so callers checking the return value for null don't blow up.
    private void installOpenShim() {
        ((JavascriptExecutor) getDriver()).executeScript("window.__opens = [];"
                + "window.open = (url, target, features) => {"
                + "  window.__opens.push("
                + "    {url: url, target: target, features: features});"
                + "  return {closed: false};" + "};");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> waitForFirstOpen() {
        return (Map<String, Object>) waitUntil(
                d -> ((JavascriptExecutor) d).executeScript(
                        "return (window.__opens && window.__opens.length)"
                                + " ? window.__opens[0] : null;"));
    }
}
