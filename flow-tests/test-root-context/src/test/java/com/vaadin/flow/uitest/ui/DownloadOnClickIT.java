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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DownloadOnClickIT extends ChromeBrowserTest {

    @Test
    public void clickSuccess_urlServesBodyWithLazyFileName() {
        open();

        String response = fetchClickedDownload("download-success");

        Assert.assertEquals(
                "200|attachment; filename=\"" + DownloadOnClickView.FILE_NAME
                        + "\"|" + DownloadOnClickView.BODY,
                response);
    }

    @Test
    public void clickFailure_urlRespondsWithServerError() {
        open();

        String response = fetchClickedDownload("download-failure");

        Assert.assertTrue("Expected a 500 response, got: " + response,
                response.startsWith("500|"));
    }

    // Replaces window.Vaadin.Flow.download.start with a recorder so no save
    // dialog opens, clicks the button and fetches the recorded URL, returning
    // "status|content-disposition|body".
    private String fetchClickedDownload(String buttonId) {
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("window.__downloads = [];"
                + "window.Vaadin.Flow.download.start = "
                + "  url => window.__downloads.push(url);");
        findElement(By.id(buttonId)).click();
        waitUntil(d -> js.executeScript(
                "return window.__downloads.length > 0 || null;"));
        return (String) js.executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                fetch(window.__downloads[0])
                  .then(r => r.text().then(t => done(r.status + '|'
                      + r.headers.get('Content-Disposition') + '|' + t)))
                  .catch(e => done('FETCH_ERROR:' + e));
                """);
    }
}
