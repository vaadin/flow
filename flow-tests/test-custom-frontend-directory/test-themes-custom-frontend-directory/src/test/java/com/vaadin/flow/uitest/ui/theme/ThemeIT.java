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

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.ImageElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.CSS_SNOWFLAKE;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.DICE_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.FONTAWESOME_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SUB_COMPONENT_ID;

public class ThemeIT extends ChromeBrowserTest {

    @Test
    public void typeScriptCssImport_stylesAreApplied() {
        getDriver().get(getRootURL() + "/path/hello");
        waitForDevServer();

        checkLogsForErrors();

        final TestBenchElement helloWorld = $(TestBenchElement.class).first()
                .findElement(By.tagName("hello-world-view"));

        Assert.assertEquals("hello-world-view", helloWorld.getTagName());

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
    public void nodeAssetInCss_pathIsSetCorrectly() {
        open();
        final String resourceUrl = getRootURL()
                + "/path/themes/app-theme/fortawesome/icons/snowflake.svg";
        WebElement cssNodeSnowflake = findElement(By.id(CSS_SNOWFLAKE));
        final String expectedImgUrl = "url(\"" + resourceUrl + "\")";
        Assert.assertEquals(
                "Background image has been referenced in styles.css and "
                        + "expected to be applied",
                expectedImgUrl,
                cssNodeSnowflake.getCssValue("background-image"));
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
    public void applicationTheme_onlyStylesCssIsApplied() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));
        // Note themes/app-theme gets VAADIN/static from the file-loader
        Assert.assertEquals("body background-image should come from styles.css",
                "url(\"" + getRootURL()
                        + "/path/VAADIN/static/themes/app-theme/img/bg.jpg\")",
                body.getCssValue("background-image"));

        Assert.assertEquals("body font-family should come from styles.css",
                "Ostrich", body.getCssValue("font-family"));

        Assert.assertEquals("html color from styles.css should be applied.",
                "rgba(0, 0, 0, 1)", body.getCssValue("color"));

        // Note themes/app-theme gets VAADIN/static from the file-loader
        getDriver().get(getRootURL()
                + "/path/VAADIN/static/themes/app-theme/img/bg.jpg");
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
    public void parentTheme_isApplied() {
        open();
        checkLogsForErrors();

        Assert.assertEquals("Color from parent theme should be applied.",
                "rgba(0, 255, 255, 1)",
                $(SpanElement.class).id(FONTAWESOME_ID).getCssValue("color"));

        Assert.assertEquals("Child theme should override parent theme values",
                "5px",
                $(SpanElement.class).id(FONTAWESOME_ID).getCssValue("margin"));

        Assert.assertEquals("Child theme values should be applied", "5px",
                $(SpanElement.class).id(FONTAWESOME_ID).getCssValue("padding"));

        TestBenchElement myField = $(TestBenchElement.class)
                .id(MY_COMPONENT_ID);

        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
        Assert.assertEquals(
                "Polymer text field should get parent border radius", "0px",
                input.getCssValue("border-radius"));

        Assert.assertEquals("Polymer text field should use green as color",
                "rgba(0, 128, 0, 1)", input.getCssValue("color"));

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

        // Note themes/app-theme gets VAADIN/static from the file-loader
        Assert.assertEquals("Imported css file URLs should have been handled.",
                "url(\"" + getRootURL()
                        + "/path/VAADIN/static/themes/app-theme/icons/archive.png\")",
                $(SpanElement.class).id(SUB_COMPONENT_ID)
                        .getCssValue("background-image"));
    }

    @Test
    public void staticModuleAsset_servedFromAppTheme() {
        open();
        checkLogsForErrors();

        Assert.assertEquals(
                "Node assets should have been copied to 'themes/app-theme'",
                getRootURL()
                        + "/path/themes/app-theme/fortawesome/icons/snowflake.svg",
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
    public void themeRulesOverrideLumo() {
        open();
        checkLogsForErrors();
        Assert.assertEquals(
                "Background should be blue, as overridden in the theme",
                "rgba(0, 0, 255, 1)",
                $("html").first().getCssValue("background-color"));

    }

    @Test
    public void documentCssImport_onlyExternalAddedToHeadAsLink() {
        open();
        checkLogsForErrors();

        final WebElement documentHead = getDriver()
                .findElement(By.xpath("/html/head"));
        final List<WebElement> links = documentHead
                .findElements(By.tagName("link"));

        List<String> linkUrls = links.stream()
                .map(link -> link.getAttribute("href"))
                .collect(Collectors.toList());

        Assert.assertTrue("Missing link for external url", linkUrls
                .contains("https://fonts.googleapis.com/css?family=Itim"));
        Assert.assertFalse("Found import that webpack should have resolved",
                linkUrls.contains("sub-css/sub.css"));
    }

    @Test
    public void customFrontendDirectory_generatedFilesNotInDefaultFrontendFolder() {
        open();

        File baseDir = new File(System.getProperty("user.dir", "."));
        File expectedGeneratedFolder = new File(baseDir,
                "side-src/main/frontend/generated");
        File defaultGeneratedFolder = new File(baseDir, "frontend/generated");

        String[] generatedFiles = { "theme.d.ts", "theme.js",
                "theme-app-theme.generated.js",
                "theme-parent-theme.generated.js", "vaadin.ts" };
        for (String generatedFile : generatedFiles) {
            Assert.assertTrue(
                    "Expecting " + generatedFile + " to be present in "
                            + expectedGeneratedFolder.getPath()
                            + ", but was not",
                    new File(expectedGeneratedFolder, generatedFile).exists());
            Assert.assertFalse(
                    "Expecting " + generatedFile + " not to be present in "
                            + defaultGeneratedFolder.getPath()
                            + ", but was not",
                    new File(defaultGeneratedFolder, generatedFile).exists());
        }
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
