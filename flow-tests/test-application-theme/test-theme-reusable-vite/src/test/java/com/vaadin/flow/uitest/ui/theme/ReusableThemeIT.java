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
package com.vaadin.flow.uitest.ui.theme;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.ImageElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.FONTAWESOME_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.KEYBOARD_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.LEMON_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.MY_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SUB_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SUN_ID;

public class ReusableThemeIT extends ChromeBrowserTest {

    @Test
    public void secondTheme_staticFilesNotCopied() {
        getDriver()
                .get(getRootURL() + "/path/themes/reusable-theme/img/bg.jpg");
        Assert.assertFalse("reusable-theme static files should be copied",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));

        getDriver().get(getRootURL() + "/path/themes/no-copy/no-copy.txt");
        String source = driver.getPageSource();
        Matcher m = Pattern.compile(
                ".*HTTP ERROR 404 Request was not handled by any registered handler.*",
                Pattern.DOTALL).matcher(source);
        Assert.assertTrue("no-copy theme should not be handled", m.matches());
    }

    @Test
    public void applicationTheme_GlobalCss_isUsed() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));
        // Note themes/reusable-theme gets VAADIN/static from the file-loader

        // Vite will make the image to something like "bg.45a07d7f.jpg" instead
        // of keeping name intact.
        final String bgCssValue = body.getCssValue("background-image");
        final String regex = ("url\\(\"" + getRootURL()
                + "/path/VAADIN/build/(.*)\\.jpg\"\\)")
                .replaceAll("/", "\\\\/");
        Matcher imageMatcher = Pattern.compile(regex).matcher(bgCssValue);

        Assert.assertTrue("BG image not found in body css '" + bgCssValue + "'",
                imageMatcher.find());

        Assert.assertEquals("Ostrich", body.getCssValue("font-family"));

        // Note themes/reusable-theme gets VAADIN/static from the file-loader
        getDriver().get(getRootURL() + "/path/VAADIN/build/"
                + imageMatcher.group(1) + ".jpg");
        Assert.assertFalse("reusable-theme background file should be served",
                driver.getPageSource().contains("Could not navigate"));
    }

    @Test
    public void applicationTheme_importCSS_isUsed() {
        open();
        checkLogsForErrors();

        Assert.assertEquals("Imported FontAwesome css file should be applied.",
                "\"Font Awesome 5 Free\"", $(SpanElement.class)
                        .id(FONTAWESOME_ID).getCssValue("font-family"));

        String iconUnicode = getCssPseudoElementValue(FONTAWESOME_ID,
                "::before");
        Assert.assertEquals(
                "Font-Icon from FontAwesome css file should be applied.",
                "\"\uf0f4\"", iconUnicode);

        getDriver().get(getRootURL()
                + "/path/VAADIN/static/@fortawesome/fontawesome-free/webfonts/fa-solid-900.svg");
        Assert.assertFalse("Font resource should be available",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    @Test
    public void componentThemeIsApplied() {
        open();
        TestBenchElement myField = $(TestBenchElement.class)
                .id(MY_COMPONENT_ID);
        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
        Assert.assertEquals("Polymer text field should have red background",
                "rgba(255, 0, 0, 1)", input.getCssValue("background-color"));
    }

    @Test
    public void subCssWithRelativePath_urlPathIsNotRelative() {
        open();
        checkLogsForErrors();

        // Note themes/reusable-theme gets VAADIN/static from the file-loader
        Assert.assertTrue("Imported css file URLs should have been handled.",
                $(SpanElement.class).id(SUB_COMPONENT_ID)
                        .getCssValue("background-image")
                        .contains("data:image/png;base64"));
    }

    @Test
    public void staticModuleAsset_servedFromAppTheme() {
        open();
        checkLogsForErrors();

        Assert.assertEquals(
                "Node assets should have been copied to 'themes/reusable-theme'",
                getRootURL()
                        + "/path/themes/reusable-theme/fortawesome/icons/snowflake.svg",
                $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"));

        open(getRootURL() + "/path/"
                + $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"));
        Assert.assertFalse("Node static icon should be available",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    @Test
    public void cssWithAssetRelativePaths_urlPathIsNotRelative() {
        open();
        String expectedIconsURL = getRootURL()
                + "/path/VAADIN/static/themes/reusable-theme/fortawesome/icons/";
        String imageUrl = $(DivElement.class).id(KEYBOARD_ID)
                .getCssValue("background-image");
        Assert.assertTrue(
                "Expecting relative asset URL to be resolved as "
                        + expectedIconsURL + "keyboard.svg but was " + imageUrl,
                imageUrl.contains(expectedIconsURL + "keyboard.svg"));

        imageUrl = $(DivElement.class).id(LEMON_ID)
                .getCssValue("background-image");
        Assert.assertTrue(
                "Expecting relative asset URL to be resolved as "
                        + expectedIconsURL + "lemon.svg but was " + imageUrl,
                imageUrl.contains(expectedIconsURL + "lemon.svg"));

        imageUrl = $(DivElement.class).id(SUN_ID)
                .getCssValue("background-image");
        Assert.assertTrue(
                "Expecting relative asset URL to be resolved as "
                        + expectedIconsURL + "sun.svg but was " + imageUrl,
                imageUrl.contains(expectedIconsURL + "sun.svg"));
    }

    @Test
    public void nonThemeDependency_urlIsNotRewritten() {
        open();
        checkLogsForErrors();

        Assert.assertEquals("Relative non theme url should not be touched",
                "url(\"" + getRootURL()
                        + "/path/test/path/monarch-butterfly.jpg\")",
                $(SpanElement.class).id(BUTTERFLY_ID)
                        .getCssValue("background-image"));

        Assert.assertEquals("Absolute non theme url should not be touched",
                "url(\"" + getRootURL() + "/octopuss.jpg\")",
                $(SpanElement.class).id(OCTOPUSS_ID)
                        .getCssValue("background-image"));

        getDriver().get(getRootURL() + "/path/test/path/monarch-butterfly.jpg");
        Assert.assertFalse("webapp resource should be served",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));

        getDriver().get(getRootURL() + "/octopuss.jpg");
        Assert.assertFalse("root resource should be served",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "path/");
    }

    private String getCssPseudoElementValue(String elementId,
            String pseudoElement) {
        String script = "return window.getComputedStyle("
                + "document.getElementById(arguments[0])"
                + ", arguments[1]).content";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return (String) js.executeScript(script, elementId, pseudoElement);
    }
}
