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

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.ComponentThemeLiveReloadView.ATTACH_IDENTIFIER;
import static com.vaadin.flow.uitest.ui.ComponentThemeLiveReloadView.THEMED_COMPONENT_ID;

@NotThreadSafe
public class ComponentThemeLiveReloadIT extends ChromeBrowserTest {

    private static final String BORDER_RADIUS = "3px";
    private static final String OTHER_BORDER_RADIUS = "6px";
    private static final String THEME_FOLDER = "frontend/themes/app-theme/";

    private File componentsDir;
    private File componentCSSFile;

    @Before
    public void init() {
        File baseDir = new File(System.getProperty("user.dir", "."));
        final File themeFolder = new File(baseDir, THEME_FOLDER);

        componentsDir = new File(themeFolder, "components");
        createDirectoryIfAbsent(componentsDir);

        componentCSSFile = new File(new File(themeFolder, "components"),
                "vaadin-text-field.css");
    }

    @After
    public void cleanUp() {
        if (componentsDir.exists()) {
            doActionAndWaitUntilLiveReloadComplete(() -> {
                deleteComponentStyles();
                deleteFile(componentsDir);
            });
        }
    }

    @Test
    public void webpackLiveReload_newComponentStylesCreatedAndDeleted_stylesUpdatedOnFly() {
        open();
        Assert.assertFalse(
                "Border radius for themed component is not expected before "
                + "applying the styles",
                isComponentCustomStyle(BORDER_RADIUS)
                || isComponentCustomStyle(OTHER_BORDER_RADIUS));

        // Live reload upon adding a new component styles file
        doActionAndWaitUntilLiveReloadComplete(
                () -> createOrUpdateComponentCSSFile(BORDER_RADIUS));
        waitUntilComponentCustomStyle(BORDER_RADIUS);

        // Live reload upon updating component styles file
        doActionAndWaitUntilLiveReloadComplete(
                () -> createOrUpdateComponentCSSFile(OTHER_BORDER_RADIUS));
        waitUntilComponentCustomStyle(OTHER_BORDER_RADIUS);

        // Live reload upon file deletion
        doActionAndWaitUntilLiveReloadComplete(this::deleteComponentStyles);
        waitUntilComponentInitialStyle();
    }

    private void waitUntilComponentInitialStyle() {
        waitUntilWithMessage(driver -> !isComponentCustomStyle(BORDER_RADIUS)
                            && !isComponentCustomStyle(OTHER_BORDER_RADIUS),
                "Wait for component initial styles timeout");
    }

    private void waitUntilComponentCustomStyle(String borderRadius) {
        waitUntilWithMessage(driver -> isComponentCustomStyle(borderRadius),
                "Wait for component custom styles timeout: " + borderRadius);
    }

    private boolean isComponentCustomStyle(String borderRadius) {
        try {
            TestBenchElement themedTextField = $(TestBenchElement.class)
                    .id(THEMED_COMPONENT_ID);
            TestBenchElement input = themedTextField.$(DivElement.class)
                    .attribute("class", "vaadin-text-field-container").first()
                    .$(DivElement.class).attribute("part", "input-field")
                    .first();
            return borderRadius.equals(input.getCssValue("border-radius"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private void createOrUpdateComponentCSSFile(String borderRadius) {
        try {
            // @formatter:off
            final String componentStyles =
                    "[part=\"input-field\"] {\n" +
                    "    border-radius: " + borderRadius + ";\n" +
                    "}";
            // @formatter:on
            FileUtils.write(componentCSSFile, componentStyles,
                    StandardCharsets.UTF_8.name());
            waitUntil(driver -> componentCSSFile.exists());
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply component styles", e);
        }
    }

    private void deleteComponentStyles() {
        deleteFile(componentCSSFile);
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

    private void createDirectoryIfAbsent(File dir) {
        if (!dir.exists() && !dir.mkdir()) {
            Assert.fail("Unable to create folder " + dir);
        }
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
