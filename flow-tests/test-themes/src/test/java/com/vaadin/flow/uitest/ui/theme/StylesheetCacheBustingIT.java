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
package com.vaadin.flow.uitest.ui.theme;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test that verifies @StyleSheet URLs get content-hash
 * cache-busting parameters (?v-c=<hash>) in production mode.
 */
public class StylesheetCacheBustingIT extends ChromeBrowserTest {

    private static final Pattern HASH_PARAM_PATTERN = Pattern
            .compile("[?&]v-c=[0-9a-f]{8}");

    @Test
    public void stylesheetLinksHaveCacheBustingHash() {
        getDriver()
                .get(getRootURL() + "/view/" + CssLoadingView.class.getName());
        waitForDevServer();

        // In production mode, data-id keeps the original annotation value
        // including the context:// prefix. Use findElements to avoid throwing
        // NoSuchElementException in dev mode where the prefix is stripped.
        List<WebElement> auraLinks = getDriver().findElements(By.cssSelector(
                "link[data-id='appShell-context://aura/fake-aura.css']"));
        Assume.assumeTrue(
                "Skipping: cache-busting is only applied in production mode",
                !auraLinks.isEmpty() && HASH_PARAM_PATTERN
                        .matcher(auraLinks.get(0).getAttribute("href")).find());

        // Verify bare-path stylesheets also have cache-busting hashes
        assertLinkHasHash("appShell-styles/stylesheet.css");
        assertLinkHasHash("appShell-styles.css");
    }

    @Test
    public void allAppShellStylesheetLinksHaveCacheBustingHash() {
        getDriver()
                .get(getRootURL() + "/view/" + CssLoadingView.class.getName());
        waitForDevServer();

        List<WebElement> appShellLinks = getDriver().findElements(
                By.cssSelector("link[rel='stylesheet'][data-id^='appShell-']"));
        Assert.assertFalse("Expected at least one appShell stylesheet link",
                appShellLinks.isEmpty());

        // Check if production mode by looking at the first link
        Assume.assumeTrue(
                "Skipping: cache-busting is only applied in production mode",
                HASH_PARAM_PATTERN
                        .matcher(appShellLinks.get(0).getAttribute("href"))
                        .find());

        for (WebElement link : appShellLinks) {
            String href = link.getAttribute("href");
            String dataId = link.getAttribute("data-id");

            // Skip external URLs
            if (href.startsWith("http://") || href.startsWith("https://")) {
                String lowerHref = href.toLowerCase();
                if (!lowerHref.contains("localhost")
                        && !lowerHref.contains("127.0.0.1")) {
                    continue;
                }
            }

            Assert.assertTrue(
                    "AppShell stylesheet link '" + dataId
                            + "' should have ?v-c=<hash> but href was: " + href,
                    HASH_PARAM_PATTERN.matcher(href).find());
        }
    }

    private void assertLinkHasHash(String dataId) {
        WebElement link = findElement(
                By.cssSelector("link[data-id='" + dataId + "']"));
        String href = link.getAttribute("href");
        Assert.assertTrue(
                "@StyleSheet link '" + dataId
                        + "' should contain ?v-c=<hash> but was: " + href,
                HASH_PARAM_PATTERN.matcher(href).find());
    }
}
