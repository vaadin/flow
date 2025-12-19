/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.ImageElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.FONTAWESOME_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.MY_COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ReusableThemeView.SUB_COMPONENT_ID;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Assert.assertEquals("url(\"" + getRootURL()
                + "/path/VAADIN/static/themes/reusable-theme/img/bg.jpg\")",
                body.getCssValue("background-image"));

        Assert.assertEquals("Ostrich", body.getCssValue("font-family"));

        // Note themes/reusable-theme gets VAADIN/static from the file-loader
        getDriver().get(getRootURL()
                + "/path/VAADIN/static/themes/reusable-theme/img/bg.jpg");
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
        Assert.assertEquals("Imported css file URLs should have been handled.",
                "url(\"" + getRootURL()
                        + "/path/VAADIN/static/themes/reusable-theme/icons/archive.png\")",
                $(SpanElement.class).id(SUB_COMPONENT_ID)
                        .getCssValue("background-image"));
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
