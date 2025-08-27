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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.ImageElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.CSS_SNOWFLAKE;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.DICE_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.FONTAWESOME_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.KEYBOARD_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.LEMON_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.LUMO_BORDER_TOP_DIV;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SUB_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SUN_ID;

public class ThemeIT extends ChromeBrowserTest {

    @Test
    public void typeScriptCssImport_stylesAreApplied() {
        getDriver().get(getRootURL() + "/path/hello");

        checkLogsForErrors();

        // This is a TypeScript view and as TestBench cannot wait for
        // the Vaadin router to complete the routing operation (the view
        // is imported dynamically) we need "waitForFirst" here
        final TestBenchElement helloWorld = $("hello-world-view")
                .waitForFirst();

        Assert.assertEquals(
                "CSS was not applied as background color was not as expected.",
                "rgba(255, 165, 0, 1)",
                helloWorld.getCssValue("background-color"));
    }

    @Test
    public void referenceResourcesOnJavaSideForStyling_stylesAreApplied() {
        open();
        final String resourceUrl = getRootURL()
                + "/path/themes/app-theme/img/dice.jpg";
        WebElement diceSpan = findElement(By.id(DICE_ID));
        final String expectedImgUrl = "url(\"" + resourceUrl + "\")";
        Assert.assertEquals(
                "Background image has been referenced on java page and "
                        + "expected to be applied",
                expectedImgUrl, diceSpan.getCssValue("background-image"));
        getDriver().get(resourceUrl);
        Assert.assertFalse("Java-side referenced resource should be served",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    @Test
    public void nodeAssetInCss_pathIsSetCorrectly() throws IOException {
        open();
        WebElement cssNodeSnowflake = findElement(By.id(CSS_SNOWFLAKE));
        String imageUrl = cssNodeSnowflake.getCssValue("background-image");
        assertImageEquals(Paths.get("target", "classes", "META-INF", "VAADIN",
                "webapp", "VAADIN", "static", "themes", "app-theme",
                "fortawesome", "icons", "snowflake.svg"), imageUrl);
    }

    @Test
    public void secondTheme_staticFilesNotCopied() {
        getDriver().get(getRootURL() + "/path/themes/app-theme/img/bg.jpg");
        Assert.assertFalse("app-theme static files should be copied",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));

        getDriver().get(getRootURL() + "/path/themes/no-copy/no-copy.txt");
        String source = driver.getPageSource();
        Matcher m = Pattern.compile(
                ".*Could not navigate to.*themes/no-copy/no-copy.txt.*",
                Pattern.DOTALL).matcher(source);
        Assert.assertTrue("no-copy theme should not be handled", m.matches());
    }

    @Test
    public void applicationTheme_onlyStylesCssIsApplied() throws IOException {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));
        // Note themes/app-theme resources are served from VAADIN/build in
        // production mode
        String imageUrl = body.getCssValue("background-image");
        assertImageEquals(Paths.get(FrontendUtils.DEFAULT_FRONTEND_DIR,
                "themes", "app-theme", "img", "bg.jpg"), imageUrl);

        Assert.assertEquals("body font-family should come from styles.css",
                "Ostrich", body.getCssValue("font-family"));

        Assert.assertEquals("html color from styles.css should be applied.",
                "rgba(0, 0, 0, 1)", body.getCssValue("color"));

        // Note themes/app-theme gets VAADIN/static from the file-loader
        getDriver().get(
                getRootURL() + "/VAADIN/static/themes/app-theme/img/bg.jpg");
        Assert.assertFalse("app-theme background file should be served",
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
    @Ignore("Enable themeComponentStyles featureflag for component theme")
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
    public void componentThemeIsNotAppliedWhenNoFeatureFlag() {
        open();
        TestBenchElement myField = $(TestBenchElement.class)
                .id(MY_COMPONENT_ID);
        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
        Assert.assertEquals(
                "Polymer text field should have white background from vaadin-base",
                "rgba(255, 255, 255, 1)",
                input.getCssValue("background-color"));
    }

    @Test
    public void subCssWithRelativePath_imageIsLoadedProperly()
            throws IOException {
        open();
        checkLogsForErrors();

        String backgroundUrl = $(SpanElement.class).id(SUB_COMPONENT_ID)
                .getCssValue("background-image");
        assertImageEquals(Paths.get(FrontendUtils.DEFAULT_FRONTEND_DIR
                + "themes/app-theme/icons/archive.png"), backgroundUrl);
    }

    @Test
    public void staticModuleAsset_servedFromAppTheme() {
        open();
        checkLogsForErrors();

        Assert.assertEquals(
                "Node assets should have been copied to 'themes/app-theme'",
                getRootURL() + "/path/VAADIN/static/npm/icons/snowflake.svg",
                $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"));

        open(getRootURL() + "/path/"
                + $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"));
        Assert.assertFalse("Node static icon should be available",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
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

    @Test
    public void documentCssImport_externalUrlLoaded() {
        open();
        checkLogsForErrors();
        waitForFont("10px Itim");
    }

    @Test
    public void documentCssImport_subImportApplied() {
        open();
        checkLogsForErrors();

        TestBenchElement subComponent = $("*").id("sub-component");
        Assert.assertEquals("Width for sub.css should have been applied",
                "10px",
                (String) executeScript(
                        "return getComputedStyle(arguments[0]).width",
                        subComponent));
    }

    @Test
    public void importCssAddedOnce() {
        open();
        assertRuleOnce(".fa-smile-beam:"); // importCss rule
    }

    @Test
    public void cssWithAssetRelativePaths_urlPathIsNotRelative() {
        open();
        // (/VAADIN/static) matches when test runs production mode
        String expectedIconsURL = ".*" + getRootURL()
                + "/path(/VAADIN/static)?/themes/app-theme/fortawesome/icons/%s.*";
        String imageUrl = $(DivElement.class).id(KEYBOARD_ID)
                .getCssValue("background-image");
        Assert.assertTrue("Expecting relative asset URL to be resolved as "
                + expectedIconsURL + "/keyboard.svg but was " + imageUrl,
                imageUrl.matches(
                        String.format(expectedIconsURL, "keyboard.svg")));

        imageUrl = $(DivElement.class).id(LEMON_ID)
                .getCssValue("background-image");
        Assert.assertTrue(
                "Expecting relative asset URL to be resolved as "
                        + expectedIconsURL + "/lemon.svg but was " + imageUrl,
                imageUrl.matches(String.format(expectedIconsURL, "lemon.svg")));

        imageUrl = $(DivElement.class).id(SUN_ID)
                .getCssValue("background-image");
        Assert.assertTrue(
                "Expecting relative asset URL to be resolved as "
                        + expectedIconsURL + "/sun.svg but was " + imageUrl,
                imageUrl.matches(String.format(expectedIconsURL, "sun.svg")));
    }

    /**
     * Main theme.json does not include utility Lumo import which has border
     * styling etc. Parent theme.json does include Lumo utility import.
     */
    @Test
    public void parentTheme_lumoStyleAppliedFromParentTheme() {
        open();
        WebElement cssNodeLumoBorderDiv = findElement(
                By.id(LUMO_BORDER_TOP_DIV));
        Assert.assertEquals("solid",
                cssNodeLumoBorderDiv.getCssValue("border-top-style"));
        Assert.assertNotEquals("0px",
                cssNodeLumoBorderDiv.getCssValue("border-top-width"));
        Assert.assertEquals("rgba(26, 57, 96, 0.1)",
                cssNodeLumoBorderDiv.getCssValue("border-top-color"));
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
