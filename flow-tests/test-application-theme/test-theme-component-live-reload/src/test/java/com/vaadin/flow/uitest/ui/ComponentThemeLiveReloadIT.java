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

import java.io.File;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.ComponentThemeLiveReloadView.ATTACH_IDENTIFIER;

@NotThreadSafe
public class ComponentThemeLiveReloadIT extends ChromeBrowserTest {

    private static final String THEMES_FOLDER = FrontendUtils.DEFAULT_FRONTEND_DIR
            + "/themes/";
    private static final String CURRENT_THEME = "app-theme";
    private static final String PARENT_THEME = "parent-theme";
    private static final String CURRENT_THEME_FOLDER = THEMES_FOLDER
            + CURRENT_THEME + "/";
    private static final String PARENT_THEME_FOLDER = THEMES_FOLDER
            + PARENT_THEME + "/";
    private static final String THEME_GENERATED_PATTERN = FrontendUtils.DEFAULT_FRONTEND_DIR
            + "/generated/theme-%s.generated.js";
    private static final String COMPONENT_STYLE_SHEET = "components/vaadin-text-field.css";

    private File currentThemeComponentCSSFile;
    private File currentThemeGeneratedFile;

    private File parentThemeComponentCSSFile;
    private File parentThemeGeneratedFile;

    @Before
    public void init() {
        File baseDir = new File(System.getProperty("user.dir", "."));

        final File currentThemeFolder = new File(baseDir, CURRENT_THEME_FOLDER);
        currentThemeComponentCSSFile = new File(currentThemeFolder,
                COMPONENT_STYLE_SHEET);
        currentThemeGeneratedFile = new File(baseDir,
                String.format(THEME_GENERATED_PATTERN, CURRENT_THEME));

        final File parentThemeFolder = new File(baseDir, PARENT_THEME_FOLDER);
        parentThemeComponentCSSFile = new File(parentThemeFolder,
                COMPONENT_STYLE_SHEET);
        parentThemeGeneratedFile = new File(baseDir,
                String.format(THEME_GENERATED_PATTERN, PARENT_THEME));
    }

    @After
    public void cleanUp() {
        if (currentThemeComponentCSSFile.exists()) {
            // This waits until live reload complete to not affect the second
            // re-run in CI (if any) and to not affect other @Test methods
            // (if any appear in the future)
            doActionAndWaitUntilLiveReloadComplete(
                    this::deleteCurrentThemeComponentStyles);
        }

        if (parentThemeComponentCSSFile.exists()) {
            // This waits until live reload complete to not affect the second
            // re-run in CI (if any) and to not affect other @Test methods
            // (if any appear in the future)
            doActionAndWaitUntilLiveReloadComplete(
                    this::deleteParentThemeComponentStyles);
        }
    }

    private void deleteCurrentThemeComponentStyles() {
        Assert.assertTrue("Expected theme generated file to be present",
                currentThemeGeneratedFile.exists());
        deleteFile(currentThemeComponentCSSFile);
    }

    private void deleteParentThemeComponentStyles() {
        Assert.assertTrue("Expected parent theme generated file to be present",
                parentThemeGeneratedFile.exists());
        deleteFile(parentThemeComponentCSSFile);
    }

    private void deleteFile(File fileToDelete) {
        if (fileToDelete != null && fileToDelete.exists()
                && !fileToDelete.delete()) {
            Assert.fail("Unable to delete " + fileToDelete);
        }
    }

    private void doActionAndWaitUntilLiveReloadComplete(Runnable action) {
        final String initialAttachId = getAttachIdentifier();
        action.run();
        waitForLiveReload(initialAttachId);
    }

    private String getAttachIdentifier() {
        int attempts = 0;
        while (attempts < 10) {
            try {
                waitForElementPresent(By.id(ATTACH_IDENTIFIER));
                return findElement(By.id(ATTACH_IDENTIFIER)).getText();
            } catch (StaleElementReferenceException
                    | NoSuchElementException e) {
                // go to next attempt
                attempts++;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Assert.fail(
                        "Test interrupted while waiting for attach identifier");
            }
        }
        Assert.fail("Attach Identifier Element waiting timeout");
        return null;
    }

    private void waitForLiveReload(final String initialAttachId) {
        waitUntilWithMessage(d -> {
            try {
                final String newViewId = getAttachIdentifier();
                return !initialAttachId.equals(newViewId);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        }, "Wait for live reload timeout");
    }

    private void waitUntilWithMessage(ExpectedCondition<?> condition,
            String message) {
        try {
            waitUntil(condition);
        } catch (TimeoutException te) {
            Assert.fail(message);
        }
    }
}
