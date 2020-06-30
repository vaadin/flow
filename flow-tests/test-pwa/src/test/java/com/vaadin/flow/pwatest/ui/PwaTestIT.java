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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.google.gwt.thirdparty.json.JSONException;
import com.google.gwt.thirdparty.json.JSONObject;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PwaTestIT extends ChromeBrowserTest {


    @Test
    public void testPwaResources() throws IOException, JSONException {
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
        JSONObject manifest = readJsonFromUrl(href);
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
        // find precache resources from service worker
        pattern = Pattern.compile("\\{ url: '([^']+)', revision: '[^']+' }");
        matcher = pattern.matcher(serviceWorkerJS);
        // Test that all precache resources are available
        while (matcher.find()) {
            Assert.assertTrue(
                    matcher.group(1) + " didn't respond with resource",
                    exists(matcher.group(1)));
        }
    }

    private void checkIcons(List<WebElement> icons, int expected) {
        Assert.assertEquals(expected, icons.size());
        for (WebElement element : icons) {
            String href = element.getAttribute("href");
            Assert.assertTrue(href + " didn't respond with resource",
                    exists(href));
        }
    }

    private boolean exists(String URLName) {
        URLName = URLName.startsWith("http") ? URLName
                : getRootURL() + "/" + URLName;
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(URLName)
                    .openConnection();
            con.setInstanceFollowRedirects(false);
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String readStringFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }
    }

    private static JSONObject readJsonFromUrl(String url)
            throws IOException, JSONException {
        return new JSONObject(readStringFromUrl(url));
    }

}
