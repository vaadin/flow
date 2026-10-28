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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Kept in this module, which runs on Jetty without frontend hotdeploy: Jetty 12
 * may throw MalformedURLException, which Flow logs as a warning, when checking
 * for a static resource before handling the download request. Under hotdeploy
 * the dev server handler rejects such paths before the download is handled.
 */
public class DownloadQuoteFileNameIT extends AbstractStreamResourceIT {

    private static final String FETCH_ERROR = "FETCH_ERROR:";

    @Test
    public void downloadWithQuoteInFileName_contentIsServed()
            throws IOException {
        open();

        String url = findElement(By.id("download-quote")).getAttribute("href");
        getDriver().manage().timeouts().scriptTimeout(Duration.ofSeconds(15));

        try (InputStream stream = download(url)) {
            Assert.assertEquals(DownloadQuoteFileNameView.CONTENT,
                    IOUtils.toString(stream, StandardCharsets.UTF_8));
        }
        // Special characters in the file name in URL are encoded.
        Assert.assertEquals("download-%22.json", FilenameUtils.getName(url));
        // The quote in the file name is escaped in the header.
        String contentDisposition = fetchContentDisposition(url);
        checkLogsForErrors();
        Assert.assertFalse(
                "Fetching the download failed: " + contentDisposition,
                contentDisposition != null
                        && contentDisposition.startsWith(FETCH_ERROR));
        Assert.assertEquals("attachment; filename=\"download-\\\".json\"",
                contentDisposition);
    }

    private String fetchContentDisposition(String url) {
        return (String) ((JavascriptExecutor) getDriver())
                .executeAsyncScript("""
                        const done = arguments[arguments.length - 1];
                        fetch(arguments[0])
                          .then(r => done(r.headers.get('Content-Disposition')))
                          .catch(e => done(arguments[1] + e));
                        """, url, FETCH_ERROR);
    }
}
