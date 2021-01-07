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
import java.util.UUID;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

@Ignore("https://github.com/vaadin/flow/issues/9746")
@NotThreadSafe
public class ThemeLiveReloadIT extends ChromeBrowserTest {

    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";
    private static final String THEME_FOLDER = "frontend/themes/app-theme/";

    private File baseDir;
    private File globalCSSFile;
    private File globalFontCSSFile;
    private File fontFile;

    @Before
    public void init() {
        baseDir = new File(System.getProperty("user.dir", "."));

        File fontsDir = new File(baseDir, THEME_FOLDER + "fonts");
        if (!fontsDir.exists() && !fontsDir.mkdir()) {
            Assert.fail("Unable to create fonts folder");
        }

        // TODO: use unique file name here, because webpack compilation fails
        // when add->delete->add a file with the same name. Perhaps, webpack
        // file caching causes this.
        // https://github.com/vaadin/flow/issues/9596
        String relativeFilePath = String.format(THEME_FOLDER + "global-%s.css",
                UUID.randomUUID().toString());
        globalCSSFile = new File(baseDir, relativeFilePath);

        // TODO: use unique file name here, because webpack compilation fails
        // when add->delete->add a file with the same name. Perhaps, webpack
        // file caching causes this.
        // https://github.com/vaadin/flow/issues/9596
        relativeFilePath = String.format(THEME_FOLDER + "global-font-%s.css",
                UUID.randomUUID().toString());
        globalFontCSSFile = new File(baseDir, relativeFilePath);

        String fontFileName = String.format("ostrich-sans-regular-%s.ttf",
                UUID.randomUUID().toString());
        fontFile = new File(baseDir, THEME_FOLDER + "fonts/" + fontFileName);
    }

    @After
    public void cleanUp() {
        deleteFile(globalCSSFile);
        deleteFile(globalFontCSSFile);
        deleteFile(fontFile);
    }

    @Test
    public void webpackLiveReload_newCssCreatedAndDeleted_stylesUpdatedOnFly()
            throws IOException {
        open();

        // Live reload upon new global.css
        final WebElement htmlElement = findElement(By.tagName("html"));
        Assert.assertNotEquals(RED_COLOR,
                htmlElement.getCssValue("background-color"));
        createGlobalCssWithBackgroundColor();
        waitUntilCustomBackgroundColor();

        // Live reload upon file deletion
        deleteFile(globalCSSFile);
        waitUntilInitialBackgroundColor();

        // TODO: deleting the CSS file reverts the styles, but still produce
        // an intermediate compile errors in logs. Perhaps, the webpack file
        // caching caused this.
        // https://github.com/vaadin/flow/issues/9596
        // checkLogsForErrors();

        // Live reload upon adding a font
        File copyFontFrom = new File(baseDir,
                "frontend/fonts/ostrich-sans-regular.ttf");

        FileUtils.copyFile(copyFontFrom, fontFile);
        waitUntil(driver -> fontFile.exists());
        createGlobalCssWithFont();
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
        final WebElement html = findElement(By.tagName("html"));
        return RED_COLOR.equals(html.getCssValue("background-color"));
    }

    private boolean isCustomFont() {
        final WebElement body = findElement(By.tagName("html"));
        return "Ostrich".equals(body.getCssValue("font-family"));
    }

    private void createGlobalCssWithBackgroundColor() throws IOException {
        final String styles = "html { background-color: " + RED_COLOR + "; }";
        FileUtils.write(globalCSSFile, styles, StandardCharsets.UTF_8.name());
    }

    private void createGlobalCssWithFont() throws IOException {
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
        FileUtils.write(globalFontCSSFile, fontStyle,
                StandardCharsets.UTF_8.name());
        waitUntil(driver -> globalFontCSSFile.exists());
    }

    private void deleteFile(File fileToDelete) {
        if (fileToDelete != null && fileToDelete.exists()
                && !fileToDelete.delete()) {
            Assert.fail("Unable to delete " + fileToDelete);
        }
    }
}
