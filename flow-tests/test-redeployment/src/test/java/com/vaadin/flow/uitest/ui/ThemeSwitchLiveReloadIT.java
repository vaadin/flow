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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@NotThreadSafe
public class ThemeSwitchLiveReloadIT extends ChromeBrowserTest {

    private static final String BLUE_COLOR = "rgba(0, 0, 255, 1)";
    private static final String ERROR_MESSAGE = "Expected theme swap from '%s' to '%s' has not been done after '%d' attempts";
    private static final int TIMEOUT = 5;
    private static final int ATTEMPTS = 5;

    private static final String APP_THEME = "app-theme";
    private static final String OTHER_THEME = "other-theme";

    @Override
    protected String getTestPath() {
        return super.getTestPath().replace("/view", "");
    }

    @Before
    @Override
    public void checkIfServerAvailable() {
        // Make sure the server is not still restarting
        waitUntil(driver -> {
            String rootUrl = getRootURL();
            try {
                super.checkIfServerAvailable();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @After
    public void cleanUp() {
        switchThemeName(OTHER_THEME, APP_THEME);
        // This wait-until is needed to not affect the second re-run in CI
        // (if any) and to not affect other @Test methods (if any appear in
        // the future)
        waitUntilAppTheme();
    }

    @Test
    public void switchThemeName_changeThemeNameAndRecompile_themeIsChangedOnFly() {
        open();
        Assert.assertFalse(
                OTHER_THEME
                        + " styles are not expected before switching the theme",
                isOtherThemeUsed());

        // Live reload upon theme name switching
        switchThemeName(APP_THEME, OTHER_THEME);
        waitUntilOtherTheme();
    }

    private void waitUntilOtherTheme() {
        waitUntilThemeSwap(
                String.format(ERROR_MESSAGE, APP_THEME, OTHER_THEME, ATTEMPTS),
                this::isOtherThemeUsed);
    }

    private void waitUntilThemeSwap(String errMessage,
            SerializableSupplier<Boolean> themeStylesSupplier) {
        int attempts = 0;
        while (attempts < ATTEMPTS) {
            getDriver().navigate().refresh();
            try {
                getCommandExecutor().waitForVaadin();
                waitUntil(driver -> themeStylesSupplier.get(), TIMEOUT);
                return;
            } catch (TimeoutException e) {
                attempts++;
            }
        }
        Assert.fail(errMessage);
    }

    private void waitUntilAppTheme() {
        waitUntilThemeSwap(
                String.format(ERROR_MESSAGE, OTHER_THEME, APP_THEME, ATTEMPTS),
                () -> !isOtherThemeUsed());
    }

    private boolean isOtherThemeUsed() {
        try {
            final WebElement html = findElement(By.tagName("html"));
            return BLUE_COLOR.equals(html.getCssValue("background-color"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private void switchThemeName(String oldThemeName, String newThemeName) {
        File baseDir = new File(System.getProperty("user.dir", "."));
        File sourcePath = Paths
                .get(baseDir.getPath(), "src", "main", "java", AppShell.class
                        .getPackage().getName().replace(".", File.separator))
                .toFile();
        File appShellClassFile = Paths
                .get(sourcePath.getPath(),
                        AppShell.class.getSimpleName().concat(".java"))
                .toFile();
        File outputPath = Paths.get(baseDir.getPath(), "target", "classes")
                .toFile();
        try {
            String content = FileUtils.readFileToString(appShellClassFile,
                    StandardCharsets.UTF_8);
            if (content.contains(oldThemeName)) {
                content = content.replace(oldThemeName, newThemeName);
                FileUtils.writeStringToFile(appShellClassFile, content,
                        StandardCharsets.UTF_8);
                recompileAppShell(appShellClassFile, sourcePath, outputPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to change theme name in AppShell class", e);
        }
    }

    private void recompileAppShell(File appShellClassFile, File sourcePath,
            File outputPath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-d", outputPath.getPath(),
                "-sourcepath", sourcePath.getPath(),
                appShellClassFile.getPath());
        Assert.assertEquals("Failed to recompile AppShell.java", 0, result);
    }
}
