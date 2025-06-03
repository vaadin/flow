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
package com.vaadin.flow.uitest.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static java.time.temporal.ChronoUnit.SECONDS;

public class DownloadHandlerIT extends AbstractStreamResourceIT {

    @Test
    public void getDynamicDownloadHandlerResource() throws IOException {
        open();

        assertDownloadedContent("download-handler-text", "file%2B.jpg");
    }

    @Test
    public void getDynamicDownloadHandlerFileResource() throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-file"));
        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                link.getAttribute("router-ignore"));
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assert.assertEquals("""
                    {
                      "download": true
                    }""", String.join("\n", lines));
        }

        Assert.assertEquals("download.json", FilenameUtils.getName(url));
    }

    @Test
    public void getDynamicDownloadHandlerClassResource() throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-class"));
        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                link.getAttribute("router-ignore"));
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assert.assertEquals("""
                    {
                      "class": "resource"
                    }""", String.join("\n", lines));
        }

        Assert.assertEquals("class-file.json", FilenameUtils.getName(url));
    }

    @Test
    public void getDynamicDownloadHandlerServletResource() throws IOException {
        open();

        WebElement link = findElement(By.id("download-handler-servlet"));
        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                link.getAttribute("router-ignore"));
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assert.assertEquals("""
                    {
                      "servlet": "resource"
                    }""", String.join("\n", lines));
        }

        Assert.assertEquals("servlet.json", FilenameUtils.getName(url));
    }

    @Test
    public void getDynamicDownloadHandlerInputStream() throws IOException {
        open();

        assertDownloadedContent("download-handler-input-stream", "");
    }

    @Test
    public void getDynamicDownloadHandlerFailingInputStream_errorIsReceived() {
        open();

        WebElement link = findElement(
                By.id("download-handler-input-stream-error"));
        link.click();

        getDriver().manage().timeouts().scriptTimeout(Duration.of(15, SECONDS));

        Assert.assertEquals("HTTP ERROR 500",
                findElement(By.className("error-code")).getText());
    }

    @Test
    public void detach_attachALink_getDynamicVaadinResource()
            throws IOException {
        open();

        findElement(By.id("detach-attach")).click();

        assertDownloadedContent("download-handler-text", "file%2B.jpg");
    }

    private void assertDownloadedContent(String downloadId, String filename)
            throws IOException {
        WebElement link = findElement(By.id(downloadId));
        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                link.getDomAttribute("router-ignore"));
        String url = link.getDomAttribute("href");

        getDriver().manage().timeouts().scriptTimeout(Duration.of(15, SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            Assert.assertEquals("foo", String.join("", lines));
        }

        Assert.assertEquals(filename, FilenameUtils.getName(url));
    }

}
