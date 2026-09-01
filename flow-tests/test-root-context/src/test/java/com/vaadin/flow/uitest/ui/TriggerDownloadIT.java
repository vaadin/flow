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
        installRecordingShim();

        findElement(By.id("download-url")).click();

        Map<String, Object> recorded = waitForFirstDownload();
        Assert.assertEquals("/static/sample.bin", recorded.get("url"));
        Assert.assertNull(recorded.get("filename"));
    }

    @Test
    public void clickUrlWithFilename_invokesStartWithBothAndQuotesRoundTrip() {
        open();
        installRecordingShim();

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
        installRecordingShim();

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
        installRecordingShim();

        WebElement field = findElement(By.id("url-source"));
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = '/from-input/file.bin';", field);

        findElement(By.id("download-input")).click();

        Map<String, Object> recorded = waitForFirstDownload();
        Assert.assertEquals("/from-input/file.bin", recorded.get("url"));
    }

    // Replace window.Vaadin.Flow.download.start with a recorder so the IT
    // never actually triggers a save dialog or navigation. Each invocation
    // pushes {url, filename} onto window.__downloads.
    private void installRecordingShim() {
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
}
