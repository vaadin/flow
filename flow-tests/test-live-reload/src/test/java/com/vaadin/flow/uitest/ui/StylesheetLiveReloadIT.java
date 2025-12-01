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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ThrowingConsumer;

import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class StylesheetLiveReloadIT extends AbstractLiveReloadIT {

    private static final String UPDATED_DIV_BG_COLOR = "rgba(0, 0, 255, 1)";
    private static final String APPSHELL_STYLES_DIV_BG_COLOR = "rgba(139, 0, 139, 1)";
    private static final String APPSHELL_IMPORTED_DIV_BG_COLOR = "rgba(184, 134, 11, 1)";
    private static final String APPSHELL_NESTED_IMPORTED_DIV_BG_COLOR = "rgba(139, 0, 0, 1)";
    private static final String VIEW_STYLES_DIV_BG_COLOR = "rgba(75, 0, 130, 1)";
    private static final String VIEW_IMPORTED_DIV_BG_COLOR = "rgba(205, 133, 63, 1)";
    private static final String VIEW_NESTED_IMPORTED_DIV_BG_COLOR = "rgba(255, 127, 80, 1)";
    private final Map<Path, byte[]> styleSheetRestore = new HashMap<>();
    private static final String DIV_BG_COLOR_BEFORE_DELETE = "rgba(0, 255, 0, 1)";

    private Path resourcesPath;

    @Before
    public void detectStylesheetsLocation() throws URISyntaxException {
        URL markerUrl = getClass().getResource("/META-INF/resources/.marker");
        Assert.assertNotNull("No marker file found", markerUrl);
        Assert.assertEquals(
                "Marker file is not a physical file: "
                        + markerUrl.getProtocol(),
                "file", markerUrl.getProtocol());
        resourcesPath = Paths.get(markerUrl.toURI()).getParent();
        Paths.get("css", "images", "vaadin-logo.png");
    }

    @After
    public void restoreStylesheets() throws IOException {
        for (Map.Entry<Path, byte[]> entry : styleSheetRestore.entrySet()) {
            System.out.println("Restoring " + entry.getKey());
            Files.write(entry.getKey(), entry.getValue());
        }
    }

    // Defining a single test method for all use cases to prevent flakyness
    // caused
    // by resource cleanup when running individual tests in parallel
    @Test
    public void stylesheetTag_updateReferencedResources_changesApplied()
            throws IOException {
        // Disables cache to make the test reliable, preventing old content to
        // be served by the servlet container even if the resource has been
        // updated
        getDevTools().setCacheDisabled(true);
        open();
        assertStyleSheetIsReloaded("appshell-style",
                APPSHELL_STYLES_DIV_BG_COLOR);
        assertStyleSheetIsReloaded("appshell-imported",
                APPSHELL_IMPORTED_DIV_BG_COLOR);
        assertStyleSheetIsReloaded("appshell-nested-imported",
                APPSHELL_NESTED_IMPORTED_DIV_BG_COLOR);

        assertStyleSheetIsReloaded("view-style", VIEW_STYLES_DIV_BG_COLOR);
        assertStyleSheetIsReloaded("view-imported", VIEW_IMPORTED_DIV_BG_COLOR);
        assertStyleSheetIsReloaded("view-nested-imported",
                VIEW_NESTED_IMPORTED_DIV_BG_COLOR);

        assertStyleSheetIsRemoved();
    }

    private void assertStyleSheetIsReloaded(String styledDivID,
            String originalBGColor) throws IOException {
        String backgroundColor = $("div").id(styledDivID)
                .getCssValue("backgroundColor");
        Assert.assertEquals(originalBGColor, backgroundColor);

        triggerReloadStyleSheet(styledDivID);

        Assert.assertEquals("Page should not be reloaded", getInitialAttachId(),
                getAttachId());

        waitUntil(d -> {
            var newBgColor = $("div").id(styledDivID)
                    .getCssValue("backgroundColor");
            return UPDATED_DIV_BG_COLOR.equals(newBgColor);
        });
    }

    private void assertStyleSheetIsRemoved() throws IOException {
        String initialBgColor = $("div").id("view-style-deleted")
                .getCssValue("backgroundColor");
        // Before deletion, the element should be styled (i.e., not the
        // default/transparent)
        Assert.assertEquals(
                "Precondition failed: element should be styled before deletion",
                DIV_BG_COLOR_BEFORE_DELETE, initialBgColor);

        triggerDelete();

        Assert.assertEquals("Page should not be reloaded", getInitialAttachId(),
                getAttachId());

        waitUntil(d -> {
            var newBgColor = $("div").id("view-style-deleted")
                    .getCssValue("backgroundColor");
            return !DIV_BG_COLOR_BEFORE_DELETE.equals(newBgColor);
        });
    }

    private void triggerReloadStyleSheet(String styledDivID)
            throws IOException {
        ThrowingConsumer<Path> updater = path -> {
            String content = Files.readString(path);
            content = content.replaceFirst("(background-color:\\s*)([^;]+);",
                    "$1" + UPDATED_DIV_BG_COLOR + ";");
            // Using a buffered writer to ensure changes are flushed to disk
            // before triggering hot reload in browser
            try (var writer = Files.newBufferedWriter(path,
                    StandardOpenOption.WRITE, StandardOpenOption.SYNC)) {
                writer.write(content);
                writer.flush();
            }
        };
        triggerReload(styledDivID, updater);
    }

    private void triggerReload(String divId, ThrowingConsumer<Path> updater)
            throws IOException {
        TestBenchElement button = $("button").id("reload-" + divId);
        String resourceRelativePath = button
                .getDomAttribute("test-resource-file-path");
        Assert.assertNotNull(
                "No test-resource-file-path attribute found for button "
                        + button,
                resourceRelativePath);

        Path resourcePath = resourcesPath
                .resolve(resourceRelativePath.replace('/', File.separatorChar));
        Assert.assertTrue("Resource file not found: " + resourcePath,
                Files.exists(resourcePath));

        styleSheetRestore.put(resourcePath, Files.readAllBytes(resourcePath));
        updater.accept(resourcePath);

        final byte[] content = Files.readAllBytes(resourcePath);
        // Make sure the servlet container returns the updated content
        waitUntilContentMatches(
                getRootURL() + "/context/" + resourceRelativePath, content);
        button.click();
    }

    private void triggerDelete() throws IOException {
        TestBenchElement button = $("button").id("delete-view-style-deleted");
        String resourceRelativePath = button
                .getDomAttribute("test-resource-file-path");
        Assert.assertNotNull(
                "No test-resource-file-path attribute found for button "
                        + button,
                resourceRelativePath);

        Path resourcePath = resourcesPath
                .resolve(resourceRelativePath.replace('/', File.separatorChar));
        Assert.assertTrue("Resource file not found: " + resourcePath,
                Files.exists(resourcePath));

        styleSheetRestore.put(resourcePath, Files.readAllBytes(resourcePath));
        Files.delete(resourcePath);

        waitUntil(driver -> !Files.exists(resourcePath));
        button.click();
    }

    private void waitUntilContentMatches(String url, byte[] expectedContent) {
        waitUntil(driver -> {
            try (InputStream is = URI.create(url).toURL().openStream()) {
                var out = new ByteArrayOutputStream();
                is.transferTo(out);
                return Arrays.equals(expectedContent, out.toByteArray());
            } catch (Exception ex) {
                return false;
            }
        });
    }
}
