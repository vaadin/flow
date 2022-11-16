/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.ImageElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.FONTAWESOME_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.MY_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SUB_COMPONENT_ID;

public class ReusableThemeIT extends ChromeBrowserTest {

    @BrowserTest
    public void secondTheme_staticFilesNotCopied() {
        getDriver()
                .get(getRootURL() + "/path/themes/reusable-theme/img/bg.jpg");
        Assertions.assertFalse(
                getDriver().getPageSource()
                        .contains("HTTP ERROR 404 Not Found"),
                "reusable-theme static files should be copied");

        getDriver().get(getRootURL() + "/path/themes/no-copy/no-copy.txt");
        String source = getDriver().getPageSource();
        Matcher m = Pattern.compile(
                ".*Could not navigate to.*themes/no-copy/no-copy.txt.*",
                Pattern.DOTALL).matcher(source);
        Assertions.assertTrue(m.matches(),
                "no-copy theme should not be handled");
    }

    @BrowserTest
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

        Assertions.assertTrue(imageMatcher.find(),
                "BG image not found in body css '" + bgCssValue + "'");

        Assertions.assertEquals("Ostrich", body.getCssValue("font-family"));

        // Note themes/reusable-theme gets VAADIN/static from the file-loader
        getDriver().get(getRootURL() + "/path/VAADIN/build/"
                + imageMatcher.group(1) + ".jpg");
        Assertions.assertFalse(
                getDriver().getPageSource().contains("Could not navigate"),
                "reusable-theme background file should be served");
    }

    @BrowserTest
    public void applicationTheme_importCSS_isUsed() {
        open();
        checkLogsForErrors();

        Assertions.assertEquals("\"Font Awesome 5 Free\"",
                $(SpanElement.class).id(FONTAWESOME_ID)
                        .getCssValue("font-family"),
                "Imported FontAwesome css file should be applied.");

        String iconUnicode = getCssPseudoElementValue(FONTAWESOME_ID,
                "::before");
        Assertions.assertEquals("\"\uf0f4\"", iconUnicode,
                "Font-Icon from FontAwesome css file should be applied.");

        getDriver().get(getRootURL()
                + "/path/VAADIN/static/@fortawesome/fontawesome-free/webfonts/fa-solid-900.svg");
        Assertions.assertFalse(
                getDriver().getPageSource()
                        .contains("HTTP ERROR 404 Not Found"),
                "Font resource should be available");
    }

    @BrowserTest
    public void componentThemeIsApplied() {
        open();
        TestBenchElement myField = $(TestBenchElement.class)
                .id(MY_COMPONENT_ID);
        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
        Assertions.assertEquals("rgba(255, 0, 0, 1)",
                input.getCssValue("background-color"),
                "Polymer text field should have red background");
    }

    @BrowserTest
    public void subCssWithRelativePath_urlPathIsNotRelative() {
        open();
        checkLogsForErrors();

        // Note themes/reusable-theme gets VAADIN/static from the file-loader
        Assertions.assertTrue(
                $(SpanElement.class).id(SUB_COMPONENT_ID)
                        .getCssValue("background-image")
                        .contains("data:image/png;base64"),
                "Imported css file URLs should have been handled.");
    }

    @BrowserTest
    public void staticModuleAsset_servedFromAppTheme() {
        open();
        checkLogsForErrors();

        Assertions.assertEquals(getRootURL()
                + "/path/themes/reusable-theme/fortawesome/icons/snowflake.svg",
                $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"),
                "Node assets should have been copied to 'themes/reusable-theme'");

        open(getRootURL() + "/path/"
                + $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"));
        Assertions.assertFalse(
                getDriver().getPageSource()
                        .contains("HTTP ERROR 404 Not Found"),
                "Node static icon should be available");
    }

    @BrowserTest
    public void nonThemeDependency_urlIsNotRewritten() {
        open();
        checkLogsForErrors();

        Assertions.assertEquals(
                "url(\"" + getRootURL()
                        + "/path/test/path/monarch-butterfly.jpg\")",
                $(SpanElement.class).id(BUTTERFLY_ID)
                        .getCssValue("background-image"),
                "Relative non theme url should not be touched");

        Assertions.assertEquals("url(\"" + getRootURL() + "/octopuss.jpg\")",
                $(SpanElement.class).id(OCTOPUSS_ID)
                        .getCssValue("background-image"),
                "Absolute non theme url should not be touched");

        getDriver().get(getRootURL() + "/path/test/path/monarch-butterfly.jpg");
        Assertions.assertFalse(
                getDriver().getPageSource()
                        .contains("HTTP ERROR 404 Not Found"),
                "webapp resource should be served");

        getDriver().get(getRootURL() + "/octopuss.jpg");
        Assertions.assertFalse(
                getDriver().getPageSource()
                        .contains("HTTP ERROR 404 Not Found"),
                "root resource should be served");
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
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        return (String) js.executeScript(script, elementId, pseudoElement);
    }
}
