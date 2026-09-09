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

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test that verifies @StyleSheet URLs get content-hash
 * cache-busting parameters (?v-c=<hash>) in production mode.
 */
public class StylesheetCacheBustingIT extends ChromeBrowserTest {

    private static final Pattern HASH_PARAM_PATTERN = Pattern
            .compile("[?&]v-c=[0-9a-f]{8}");

    /**
     * The stylesheets declared on {@link AppShell}. Production mode omits the
     * {@code data-id}/{@code data-file-path} attributes, so the links are
     * identified by the path they resolve to instead. The test app uses the
     * default servlet mapping, which makes {@code <base href>} the context root
     * and these paths stable.
     */
    private static final List<String> APP_SHELL_STYLESHEET_PATHS = List.of(
            "/aura/fake-aura.css", "/styles.css", "/relurl-test/styles.css");

    @Test
    public void stylesheetLinksHaveCacheBustingHash() {
        openCssLoadingView();
        Assume.assumeTrue(
                "Skipping: cache-busting is only applied in production mode",
                isProductionMode());

        // Verify the context:// stylesheet has a cache-busting hash
        assertStylesheetHasHash("/styles.css");
    }

    @Test
    public void allAppShellStylesheetLinksHaveCacheBustingHash() {
        openCssLoadingView();
        Assume.assumeTrue(
                "Skipping: cache-busting is only applied in production mode",
                isProductionMode());

        APP_SHELL_STYLESHEET_PATHS.forEach(this::assertStylesheetHasHash);
    }

    private void openCssLoadingView() {
        getDriver()
                .get(getRootURL() + "/view/" + CssLoadingView.class.getName());
        waitForDevServer();
    }

    private boolean isProductionMode() {
        return getDriver().findElements(By.tagName("vaadin-dev-tools"))
                .isEmpty();
    }

    private void assertStylesheetHasHash(String expectedPath) {
        List<String> hrefs = getDriver()
                .findElements(By.cssSelector("link[rel='stylesheet']")).stream()
                .map(link -> link.getAttribute("href")).toList();
        // getAttribute("href") returns the resolved absolute URL, so compare
        // the path for equality rather than by suffix:
        // '/relurl-test/styles.css'
        // also ends with '/styles.css'
        List<String> matches = hrefs.stream()
                .filter(href -> expectedPath.equals(pathOf(href))).toList();

        Assert.assertEquals("Expected exactly one stylesheet link with path "
                + expectedPath + ", but page had " + hrefs, 1, matches.size());
        Assert.assertTrue("@StyleSheet link '" + expectedPath
                + "' should contain ?v-c=<hash> but was: " + matches.get(0),
                HASH_PARAM_PATTERN.matcher(matches.get(0)).find());
    }

    private static String pathOf(String href) {
        try {
            return URI.create(href).getPath();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
