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
package com.vaadin.flow.test.components;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static java.time.temporal.ChronoUnit.SECONDS;

@TestFor(DownloadHandlerView.class)
public class DownloadHandlerIT extends AbstractStreamResourceIT {

    @BrowserTest
    public void getDynamicDownloadHandlerResource() throws IOException {
        open();

        assertDownloadedContent("download-handler-text", "file%2B.jpg");
    }

    @BrowserTest
    public void getDynamicDownloadHandlerFileResource() throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-file"));
        Assertions.assertEquals("", link.getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("""
                    {
                      "download": true
                    }""", String.join("\n", lines));
        }

        Assertions.assertEquals("download.json", FilenameUtils.getName(url));
    }

    @BrowserTest
    public void getDynamicDownloadHandlerFileResourceUnicodeName()
            throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-file-unicode"));
        Assertions.assertEquals("", link.getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("""
                    {
                      "download": true
                    }""", String.join("\n", lines));
        }
        // Special characters in the file name in URL are encoded.
        Assertions.assertEquals(
                "download-%C5%98%C5%99%C3%BC%C3%B1%C3%AE%C3%A7%C3%B8d%C3%AB%201%E4%B8%AD%E6%96%87.json",
                FilenameUtils.getName(url));
    }

    // jetty 12 may throw MalformedURLException which Flow logs as a warning
    // when checking for static resource before handling the download request.
    @Disabled("In dev mode with frontend hotdeploy the dev server handler rejects /VAADIN/ paths that contain a quote with 403 before the stream handler sees them")
    @BrowserTest
    public void getDynamicDownloadHandlerFileResourceUnicodeNameWithQuote()
            throws IOException {
        open();

        WebElement link = findElement(
                By.id("download-handler-file-unicode-quote"));
        Assertions.assertEquals("", link.getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("""
                    {
                      "download": true
                    }""", String.join("\n", lines));
        }
        // Special characters in the file name in URL are encoded.
        Assertions.assertEquals("download-%22.json",
                FilenameUtils.getName(url));
    }

    @BrowserTest
    public void getDynamicDownloadHandlerFileResource_rangeRequested_partialContentReceived() {
        open();

        String url = findElement(By.id("download-handler-file"))
                .getAttribute("href");
        Map<?, ?> response = (Map<?, ?>) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        """
                                const done = arguments[arguments.length - 1];
                                fetch(arguments[0], { headers: { Range: 'bytes=4-13' } })
                                  .then(response => response.text().then(body => done({
                                    status: response.status,
                                    contentRange: response.headers.get('Content-Range'),
                                    body
                                  })));
                                """,
                        url);

        Assertions.assertEquals(206L, response.get("status"));
        Assertions.assertEquals("bytes 4-13/22", response.get("contentRange"));
        Assertions.assertEquals("\"download\"", response.get("body"));
    }

    @BrowserTest
    public void withContentType_servedWithItUnlessDownloadResponseHasOne() {
        open();

        Assertions.assertEquals("text/vtt",
                fetchContentType("download-handler-content-type"));
        Assertions.assertEquals("text/plain",
                fetchContentType("download-handler-response-content-type"));
    }

    @BrowserTest
    public void getDynamicDownloadHandlerClassResource() throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-class"));
        Assertions.assertEquals("", link.getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("""
                    {
                      "class": "resource"
                    }""", String.join("\n", lines));
        }

        Assertions.assertEquals("class-file.json", FilenameUtils.getName(url));
    }

    @BrowserTest
    public void getDynamicDownloadHandlerServletResource() throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-servlet"));
        Assertions.assertEquals("", link.getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("""
                    {
                      "servlet": "resource"
                    }""", String.join("\n", lines));
        }

        Assertions.assertEquals("servlet.json", FilenameUtils.getName(url));
    }

    @BrowserTest
    public void getDynamicDownloadHandlerInputStream() throws IOException {
        open();

        assertDownloadedContent("download-handler-input-stream", "");
    }

    @BrowserTest
    public void getDynamicDownloadHandlerFailingInputStream_errorIsReceived() {
        open();

        WebElement link = findElement(
                By.id("download-handler-input-stream-error"));
        link.click();

        getDriver().manage().timeouts().scriptTimeout(Duration.of(15, SECONDS));

        Assertions.assertEquals("HTTP ERROR 500",
                findElement(By.className("error-code")).getText());
    }

    @BrowserTest
    public void getDynamicDownloadHandlerFailingInputStream_exceptionIsReceived() {
        open();

        WebElement link = findElement(
                By.id("download-handler-input-stream-exception"));
        link.click();

        getDriver().manage().timeouts().scriptTimeout(Duration.of(15, SECONDS));
        // download handler completion should set status to 403
        Assertions.assertEquals("HTTP ERROR 403",
                findElement(By.className("error-code")).getText());
    }

    @BrowserTest
    public void getDynamicDownloadHandlerFailingInputStreamCallback_errorIsReceived() {
        open();

        getDriver().manage().timeouts().scriptTimeout(Duration.of(15, SECONDS));

        // The exception escapes to the servlet container, which renders its
        // own error page, so only the status is container independent
        Assertions.assertEquals(500,
                fetchStatus("download-handler-input-stream-callback-error"));
    }

    @BrowserTest
    public void getDynamicDownloadHandlerPercentResource() throws IOException {
        open();

        assertDownloadedContent("download-handler-percent", "file_.jpg");
    }

    @BrowserTest
    public void detach_attachALink_getDynamicVaadinResource()
            throws IOException {
        open();

        findElement(By.id("detach-attach")).click();

        assertDownloadedContent("download-handler-text", "file%2B.jpg");
    }

    /**
     * Fetches the download of the given link and returns its HTTP status.
     */
    private long fetchStatus(String downloadId) {
        String url = findElement(By.id(downloadId)).getAttribute("href");
        return (Long) ((JavascriptExecutor) getDriver()).executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                fetch(arguments[0]).then(response => done(response.status));
                """, url);
    }

    /**
     * Fetches the download of the given link and returns its media type,
     * without parameters such as a charset that the container may add.
     */
    private String fetchContentType(String downloadId) {
        String url = findElement(By.id(downloadId)).getAttribute("href");
        String contentType = (String) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        """
                                const done = arguments[arguments.length - 1];
                                fetch(arguments[0])
                                  .then(response => done(response.headers.get('Content-Type')));
                                """,
                        url);
        return contentType.split(";")[0].trim();
    }

    private void assertDownloadedContent(String downloadId, String filename)
            throws IOException {
        WebElement link = findElement(By.id(downloadId));
        Assertions.assertEquals("", link.getDomAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
        String url = link.getDomAttribute("href");

        getDriver().manage().timeouts().scriptTimeout(Duration.of(15, SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("foo", String.join("", lines));
        }

        Assertions.assertEquals(filename, FilenameUtils.getName(url));
    }

}
