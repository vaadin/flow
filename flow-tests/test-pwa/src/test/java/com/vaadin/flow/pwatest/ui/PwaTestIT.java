/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.mobile.NetworkConnection;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class PwaTestIT extends ChromeDeviceTest {

    @Test
    public void testPwaResources() throws IOException {
        open();
        WebElement head = findElement(By.tagName("head"));

        // test mobile capable
        Assert.assertEquals(1, head
                .findElements(By.name("apple-mobile-web-app-capable")).size());

        // test theme color
        Assert.assertEquals(
                1, head
                        .findElements(By
                                .xpath("//meta[@name='theme-color'][@content='"
                                        + ParentLayout.THEME_COLOR + "']"))
                        .size());

        // test theme color for apple mobile
        Assert.assertEquals(1, head.findElements(
                By.xpath("//meta[@name='apple-mobile-web-app-status-bar-style']"
                        + "[@content='" + ParentLayout.THEME_COLOR + "']"))
                .size());
        // icons test
        checkIcons(head.findElements(By.xpath("//link[@rel='shortcut icon']")),
                1);

        checkIcons(head.findElements(
                By.xpath("//link[@rel='icon'][@sizes][@href]")), 2);

        checkIcons(head.findElements(
                By.xpath("//link[@rel='apple-touch-icon'][@sizes][@href]")), 1);

        checkIcons(head.findElements(By.xpath(
                "//link[@rel='apple-touch-startup-image'][@sizes][@href]")), 4);

        // test web manifest
        List<WebElement> elements = head
                .findElements(By.xpath("//link[@rel='manifest'][@href]"));
        Assert.assertEquals(1, elements.size());
        String href = elements.get(0).getAttribute("href");
        Assert.assertTrue(href + " didn't respond with resource", exists(href));
        // Verify user values in manifest.webmanifest
        JsonObject manifest = readJsonFromUrl(href);
        Assert.assertEquals(ParentLayout.PWA_NAME, manifest.getString("name"));
        Assert.assertEquals(ParentLayout.PWA_SHORT_NAME,
                manifest.getString("short_name"));
        Assert.assertEquals(ParentLayout.BG_COLOR,
                manifest.getString("background_color"));
        Assert.assertEquals(ParentLayout.THEME_COLOR,
                manifest.getString("theme_color"));

        // test service worker initialization
        elements = head.findElements(By.tagName("script")).stream()
                .filter(webElement -> webElement.getAttribute("innerHTML")
                        .startsWith("if ('serviceWorker' in navigator)"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, elements.size());

        String serviceWorkerInit = elements.get(0).getAttribute("innerHTML");
        Pattern pattern = Pattern
                .compile("navigator.serviceWorker.register\\('([^']+)'\\)");
        Matcher matcher = pattern.matcher(serviceWorkerInit);
        Assert.assertTrue("Service worker initialization missing",
                matcher.find());

        String serviceWorkerUrl = matcher.group(1).startsWith("http")
                ? matcher.group(1)
                : getRootURL() + "/" + matcher.group(1);

        Assert.assertTrue("Service worker not served at: " + serviceWorkerUrl,
                exists(serviceWorkerUrl));

        String serviceWorkerJS = readStringFromUrl(serviceWorkerUrl);
        // parse the precache resources (the app bundles) from service worker JS
        pattern = Pattern.compile("\\{'revision':('[^']+'|null),'url':'([^']+)'}");
        matcher = pattern.matcher(serviceWorkerJS);
        ArrayList<String> precacheUrls = new ArrayList<>();
        while (matcher.find()) {
            precacheUrls.add(matcher.group(2));
        }
        Assert.assertFalse("Expected at least one precache URL", precacheUrls.isEmpty());
        checkResources(precacheUrls.toArray(new String[] {}));
        checkResources("yes.png", "offline.html");
    }

    @Test
    public void testPwaResourcesOffline() throws IOException {
        open();
        waitForServiceWorkerReady();
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
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
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void testPwaOfflinePath() throws IOException {
        open();
        waitForServiceWorkerReady();

        // Confirm that app shell is loaded
        Assert.assertNotNull("Should have outlet when loaded online",
                findElement(By.id("outlet")));

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

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
            Assert.assertEquals(ParentLayout.PWA_NAME,
                    executeScript("return arguments[0].textContent", title));
            Assert.assertEquals(ParentLayout.PWA_NAME,
                    executeScript("return document.title;"));

            // Assert default offline.html page contents
            WebElement body = findElement(By.tagName("body"));
            Assert.assertTrue("Should not have outlet when loaded offline",
                    body.findElements(By.id("outlet")).isEmpty());

            WebElement offline = body.findElement(By.id("offline"));
            Assert.assertEquals(ParentLayout.PWA_NAME,
                    offline.findElement(By.tagName("h1")).getText());
            WebElement message = offline.findElement(By.className("message"));
            Assert.assertTrue("Should have “offline” in message",
                    message.getText().toLowerCase().contains("offline"));
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void compareUncompressedAndCompressedServiceWorkerJS() throws IOException {
        // test only in production mode
        Assume.assumeTrue(isProductionMode());

        byte[] uncompressed = readBytesFromUrl(getRootURL() + "/sw.js");
        byte[] compressed = readBytesFromUrl(getRootURL() + "/sw.js.gz");
        byte[] decompressed = readAllBytes(
                new GZIPInputStream(new ByteArrayInputStream(compressed)));
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
            Assert.assertTrue(href + " didn't respond with resource",
                    exists(href));
        }
    }

    private void checkResources(String... resources) {
        for (String url : resources) {
            Assert.assertTrue(url + " didn't respond with resource",
                    exists(url));
        }
    }

    private boolean exists(String url) {
        // If the mimetype can be guessed from the file name, check consistency
        // with the actual served file
        String expectedMimeType = URLConnection
                .guessContentTypeFromName(url);
        String script = "const mimeType = arguments[0];"
                + "const resolve = arguments[1];"
                + "fetch('" + url + "', {method: 'HEAD'})"
                + ".then(response => resolve(response.status===200"
                + "      && !response.redirected"
                + "      && (mimeType===null || response.headers.get('Content-Type')===mimeType)))"
                + ".catch(err => resolve(false));";
        return (boolean) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(script, expectedMimeType);
    }

    private static String readStringFromUrl(String url) throws IOException {
        return new String(readAllBytes(new URL(url).openStream()), StandardCharsets.UTF_8);
    }

    private static JsonObject readJsonFromUrl(String url)
            throws IOException {
        return Json.parse(readStringFromUrl(url));
    }

    private static byte[] readBytesFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return readAllBytes(is);
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
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
        JsonObject stats = readJsonFromUrl(getRootURL() + "/VAADIN/stats.json?v-r=init");
        return stats.getObject("appConfig").getBoolean("productionMode");
    }
}
