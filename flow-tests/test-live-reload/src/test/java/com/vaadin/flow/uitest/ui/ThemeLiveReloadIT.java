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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ThemeLiveReloadIT extends AbstractLiveReloadIT {

    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";

    @Before
    public void init() {
        File fontsDir = new File("frontend/themes/app-theme/fonts");
        if (!fontsDir.exists() && !fontsDir.mkdir()) {
            Assert.fail("Unable to create fonts folder");
        }
    }

    @After
    public void cleanUp() {
        deleteFile("frontend/themes/app-theme/global.css");
        deleteFile("frontend/themes/app-theme/global-font.css");
        deleteFile("frontend/themes/app-theme/fonts/ostrich-sans-regular.ttf");
    }

    @Test
    public void webpackLiveReload_newCssCreatedAndDeleted_stylesUpdatedOnFly()
            throws IOException {
        open();
        checkLogsForErrors();

        // Live reload upon new global.css
        final WebElement htmlElement = findElement(By.tagName("html"));
        Assert.assertNotEquals(RED_COLOR,
                htmlElement.getCssValue("background-color"));
        createGlobalCssWithBackgroundColor();
        waitUntilCustomBackgroundColor();

        // Live reload upon file deletion
        deleteFile("frontend/themes/app-theme/global.css");
        waitUntilInitialBackgroundColor();

        // TODO: deleting the CSS file reverts the styles, but still produce
        // an intermediate compile errors in logs. Perhaps, the webpack file
        // caching caused this.
        // https://github.com/vaadin/flow/issues/9596
        // checkLogsForErrors();

        // Live reload upon adding a font
        File copyFontFrom = new File("frontend/fonts/ostrich-sans-regular.ttf");
        File copyFontTo = new File(
                "frontend/themes/app-theme/fonts/ostrich-sans-regular.ttf");
        FileUtils.copyFile(copyFontFrom, copyFontTo);
        waitUntil(driver -> copyFontTo.exists());
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
        File globalCssFile = new File("frontend/themes/app-theme/global.css");
        FileUtils.write(globalCssFile, styles, StandardCharsets.UTF_8.name());
    }

    private void createGlobalCssWithFont() throws IOException {
        // @formatter:off
        final String fontStyle = 
                "@font-face {" +
                "    font-family: \"Ostrich\";" +
                "    src: url(\"./fonts/ostrich-sans-regular.ttf\") format(\"TrueType\");" +
                "}" + 
                "html {" +
                "    font-family: \"Ostrich\";" + 
                "}";
        // @formatter:on
        // TODO: use another file name here, because webpack compilation fails
        //  when add->delete->add a file with the same path. Perhaps, webpack
        //  file caching causes this.
        // https://github.com/vaadin/flow/issues/9596
        File globalCssFile = new File("frontend/themes/app-theme/global-font.css");
        FileUtils.write(globalCssFile, fontStyle,
                StandardCharsets.UTF_8.name());
        waitUntil(driver -> globalCssFile.exists());
    }

    private void deleteFile(String filePath) {
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists() && !fileToDelete.delete()) {
            Assert.fail("Unable to delete " + filePath);
        }
    }
}
