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
package com.vaadin.flow.pwatest.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.brotli.dec.BrotliInputStream;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeDeviceTest;

import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ObjectNode;

public class PwaTestIT extends ChromeDeviceTest {

    @Test
    public void testPwaResources() throws IOException {
        open();

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
        WebElement head = findElement(By.tagName("head"));

        // test mobile capable
        Assert.assertEquals(1, head
                .findElements(By.name("apple-mobile-web-app-capable")).size());

        // test theme color
        Assert.assertEquals(1,
                head.findElements(
                        By.xpath("//meta[@name='theme-color'][@content='"
                                + AppShell.THEME_COLOR + "']"))
                        .size());

        // test theme color for apple mobile
        Assert.assertEquals(1, head
                .findElements(By.xpath(
                        "//meta[@name='apple-mobile-web-app-status-bar-style']"
                                + "[@content='" + AppShell.THEME_COLOR + "']"))
                .size());
        // icons test
        checkIcons(head.findElements(By.xpath("//link[@rel='shortcut icon']")),
                1);

        checkIcons(head.findElements(
                By.xpath("//link[@rel='icon'][@sizes][@href]")), 2);

        checkIcons(head.findElements(
                By.xpath("//link[@rel='apple-touch-icon'][@sizes][@href]")), 1);

        checkIcons(head.findElements(By.xpath(
                "//link[@rel='apple-touch-startup-image'][@sizes][@href]")),
                26);

        // test web manifest
        List<WebElement> elements = head
                .findElements(By.xpath("//link[@rel='manifest'][@href]"));
        Assert.assertEquals(1, elements.size());
        String href = elements.get(0).getAttribute("href");
        assertExists(href);
        // Verify user values in manifest.webmanifest
        if (!href.startsWith(getRootURL())) {
            href = getRootURL() + '/' + href;
        }
        ObjectNode manifest = readJsonFromUrl(href);
        Assert.assertEquals(AppShell.PWA_NAME, manifest.get("name").asText());
        Assert.assertEquals(AppShell.PWA_SHORT_NAME,
                manifest.get("short_name").asText());
        Assert.assertEquals(AppShell.BG_COLOR,
                manifest.get("background_color").asText());
        Assert.assertEquals(AppShell.THEME_COLOR,
                manifest.get("theme_color").asText());

        // test service worker initialization
        elements = head.findElements(By.tagName("script")).stream()
                .filter(webElement -> getInnerHtml(webElement)
                        .startsWith("if ('serviceWorker' in navigator)"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, elements.size());

        String serviceWorkerInit = getInnerHtml(elements.get(0));
        Pattern pattern = Pattern
                .compile("navigator.serviceWorker.register\\('([^']+)'\\)");
        Matcher matcher = pattern.matcher(serviceWorkerInit);
        Assert.assertTrue("Service worker initialization missing",
                matcher.find());

        String serviceWorkerUrl = matcher.group(1).startsWith("http")
                ? matcher.group(1)
                : getRootURL() + "/" + matcher.group(1);

        assertExists(serviceWorkerUrl);

        String serviceWorkerJS = readStringFromUrl(serviceWorkerUrl);

        // For Vite search for the precache file as it is loaded at runtime
        // and not compiled into sw.js during build
        Assert.assertTrue(
                "Expected sw-runtime-resources-precache.js to be imported, but was not",
                serviceWorkerJS.contains(
                        "importScripts(\"sw-runtime-resources-precache.js\")"));

        serviceWorkerUrl = getRootURL() + "/sw-runtime-resources-precache.js";
        serviceWorkerJS = readStringFromUrl(serviceWorkerUrl);
        System.out.println(serviceWorkerJS);

        // parse the precache resources (the app bundles) from service worker JS
        pattern = Pattern.compile(
                "\\{ url: '([^']+)', revision: ('[^']+'|null) }",
                Pattern.MULTILINE);
        matcher = pattern.matcher(serviceWorkerJS);
        ArrayList<String> precacheUrls = new ArrayList<>();
        while (matcher.find()) {
            precacheUrls.add(matcher.group(2));
        }
        Assert.assertFalse("Expected at least one precache URL",
                precacheUrls.isEmpty());
        // Vite does not precache appshell if there's an offline path configured
        Assert.assertFalse("Expected appshell not to be precached",
                precacheUrls.contains("."));
        checkResources(precacheUrls.toArray(new String[] {}));
        checkResources("yes.png", "offline.html");
    }

    @Test
    public void testPwaResourcesOffline() {
        open();
        waitForServiceWorkerReady();
        getDevTools().setOfflineEnabled(true);
        try {
            // Ensure we are offline
            Assert.assertEquals("navigator.onLine should be false", false,
                    executeScript("return navigator.onLine"));

            // Check the that one icon, a file served from '/public' and
            // offline.html can be loaded from cache. In principle we should
            // check all files checked in testPwaResources, however, currently
            // not all icons are precached.
            checkResources("icons/icon-32x32.png", "yes.png", "offline.html");
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void testPwaOfflinePath() {
        open();
        waitForServiceWorkerReady();

        // Confirm that app shell is loaded
        Assert.assertNotNull("Should have outlet when loaded online",
                findElement(By.id("outlet")));

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

            // Assert page title
            waitForElementPresent(By.tagName("head"));
            WebElement head = findElement(By.tagName("head"));
            waitForElementPresent(By.tagName("title"));
            WebElement title = head.findElement(By.tagName("title"));
            Assert.assertEquals(AppShell.PWA_NAME,
                    executeScript("return arguments[0].textContent", title));
            Assert.assertEquals(AppShell.PWA_NAME,
                    executeScript("return document.title;"));

            // Assert default offline.html page contents
            WebElement body = findElement(By.tagName("body"));
            Assert.assertTrue("Should not have outlet when loaded offline",
                    body.findElements(By.id("outlet")).isEmpty());

            WebElement offline = body.findElement(By.id("offline"));
            Assert.assertEquals(AppShell.PWA_NAME,
                    offline.findElement(By.tagName("h1")).getText());
            WebElement message = offline.findElement(By.className("message"));
            Assert.assertTrue("Should have “offline” in message",
                    message.getText().toLowerCase().contains("offline"));
        } finally {
            // Reset network conditions back
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void compareUncompressedAndCompressedServiceWorkerJS()
            throws IOException {
        open();
        waitForServiceWorkerReady();

        // test only in production mode
        Assume.assumeTrue(isProductionMode());

        byte[] uncompressed = readBytesFromUrl(getRootURL() + "/sw.js");
        byte[] compressed = readBytesFromUrl(getRootURL() + "/sw.js.br");
        byte[] decompressed = readAllBytes(
                new BrotliInputStream(new ByteArrayInputStream(compressed)));
        Assert.assertArrayEquals(uncompressed, decompressed);
    }

    @Override
    protected String getTestPath() {
        return "";
    }

    private void checkIcons(List<WebElement> icons, int expected) {
        Assert.assertEquals(expected, icons.size());
        for (WebElement element : icons) {
            String href = element.getAttribute("href");
            assertExists(href);
        }
    }

    private void checkResources(String... resources) {
        for (String url : resources) {
            assertExists(url);
        }
    }

    private void assertExists(String url) {
        // If the mimetype can be guessed from the file name, check consistency
        // with the actual served file
        String expectedMimeType = URLConnection.guessContentTypeFromName(url);
        String script = "const mimeType = arguments[0];"
                + "const resolve = arguments[2];" //
                + "fetch(arguments[1], {method: 'GET'})" //
                + ".then(response => resolve({status: response.status," //
                + "      redirected: response.redirected," //
                + "      mimeType: response.headers.get('Content-Type')}))" //
                + ".catch(err => resolve());";
        Map data = (Map) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(script, expectedMimeType, url);

        if (expectedMimeType != null) {
            String mimeType = ((String) data.get("mimeType"))
                    .replaceAll(";[ ]?charset=utf-8", "");
            // Jetty is using text/javascript starting from 11.0.14, Vite uses
            // application/javascript when in dev mode
            mimeType = mimeType.replace("application/javascript",
                    "text/javascript");
            Assert.assertEquals(url + " has an unexpected mime type",
                    expectedMimeType, mimeType);
        }
        Assert.assertEquals(url + " has an unexpected redirect", false,
                data.get("redirected"));
        Assert.assertEquals(url + " has an unexpected response code", 200L,
                data.get("status"));

    }

    private static String readStringFromUrl(String url) throws IOException {
        return new String(readAllBytes(new URL(url).openStream()),
                StandardCharsets.UTF_8);
    }

    private static ObjectNode readJsonFromUrl(String url) throws IOException {
        return JacksonUtils.readTree(readStringFromUrl(url));
    }

    private static byte[] readBytesFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return readAllBytes(is);
        }
    }

    private static byte[] readAllBytes(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int count;
        byte[] data = new byte[1024];
        while ((count = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, count);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private boolean isProductionMode() throws IOException {
        ObjectNode stats = readJsonFromUrl(
                getRootURL() + "?v-r=init&location=");
        return ((ObjectNode) stats.get("appConfig")).get("productionMode").asBoolean();
    }

    private String getInnerHtml(WebElement element) {
        Object result = getCommandExecutor()
                .executeScript("return arguments[0].innerHTML;", element);
        return result == null ? "" : result.toString();
    }
}
