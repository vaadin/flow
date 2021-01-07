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

package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

@NotThreadSafe
public class ThemeLiveReloadIT extends ChromeBrowserTest {

    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";
    private static final String THEME_FOLDER = "frontend/themes/app-theme/";

    private File baseDir;
    private File stylesCSSFile;
    private File fontFile;

    @Before
    public void init() {
        baseDir = new File(System.getProperty("user.dir", "."));
        final File themeFolder = new File(baseDir, THEME_FOLDER);

        File fontsDir = new File(themeFolder, "fonts");
        if (!fontsDir.exists() && !fontsDir.mkdir()) {
            Assert.fail("Unable to create fonts folder");
        }

        stylesCSSFile = new File(themeFolder, "styles.css");
        fontFile = new File(themeFolder, "fonts/ostrich-sans-regular.ttf");
    }

    @After
    public void cleanUp() {
        doActionAndWaitUntilLiveReloadComplete((ignore) -> {
            try {
                // Cleanup the default 'styles.css' file
                FileUtils.write(stylesCSSFile, "",
                    StandardCharsets.UTF_8.name());
                deleteFile(fontFile);
            } catch (IOException e) {
                Assert.fail("Couldn't cleanup test files: " + e.getMessage());
            }
        });
    }

    @Test
    public void webpackLiveReload_newCssCreatedAndDeleted_stylesUpdatedOnFly()
        throws IOException {
        open();

        // Live reload upon new styles.css
        final WebElement htmlElement = findElement(By.tagName("html"));
        Assert.assertNotEquals(RED_COLOR,
            htmlElement.getCssValue("background-color"));
        doActionAndWaitUntilLiveReloadComplete(
            (ignore) -> addBackgroundColorToStylesCSS());
        waitUntilCustomBackgroundColor();

        // Live reload upon file deletion
        doActionAndWaitUntilLiveReloadComplete((ignore) ->
        deleteFile(stylesCSSFile));
        waitUntilInitialBackgroundColor();

        // Live reload upon adding a font
        File copyFontFrom = new File(baseDir,
            "frontend/fonts/ostrich-sans-regular.ttf");

        FileUtils.copyFile(copyFontFrom, fontFile);
        waitUntil(driver -> fontFile.exists());
        doActionAndWaitUntilLiveReloadComplete((ignore) ->
            createStylesCssWithFont());
        waitUntilCustomFont();
    }

    private void waitUntilCustomBackgroundColor() {
        waitUntil(driver -> isCustomBackGroundColor());
    }

    private void waitUntilInitialBackgroundColor() {
        waitUntil(driver -> !isCustomBackGroundColor());
    }

    private void waitUntilCustomFont() {
        waitUntil(driver -> isCustomFont());
    }

    private boolean isCustomBackGroundColor() {
        try {
            final WebElement html = findElement(By.tagName("html"));
            return RED_COLOR.equals(html.getCssValue("background-color"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean isCustomFont() {
        try {
            final WebElement body = findElement(By.tagName("html"));
            return "Ostrich".equals(body.getCssValue("font-family"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private void addBackgroundColorToStylesCSS() {
        try {
            final String styles = "html { background-color: " + RED_COLOR + "; }";
            FileUtils.write(stylesCSSFile, styles,
                StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createStylesCssWithFont() {
        try {
            // @formatter:off
            final String fontStyle =
                    "@font-face {" +
                    "    font-family: \"Ostrich\";" +
                    "    src: url(\"./fonts/" + fontFile.getName() + "\") format(\"TrueType\");" +
                    "}" +
                    "html {" +
                    "    font-family: \"Ostrich\";" +
                    "}";
            // @formatter:on
            FileUtils.write(stylesCSSFile, fontStyle,
                StandardCharsets.UTF_8.name());
            waitUntil(driver -> stylesCSSFile.exists());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile(File fileToDelete) {
        if (fileToDelete != null && fileToDelete.exists() && !fileToDelete
            .delete()) {
            Assert.fail("Unable to delete " + fileToDelete);
        }
    }

    private void doActionAndWaitUntilLiveReloadComplete(Consumer<Void> action) {
        // Add a new active client with 'blocker' key and let the
        // waitForVaadin() to block until new page/document will be loaded as a
        // result of live reload.
        executeScript(
                "window.Vaadin.Flow.clients[\"blocker\"] = {isActive: () => true};");
        action.accept(null);
        getCommandExecutor().waitForVaadin();
    }
}
