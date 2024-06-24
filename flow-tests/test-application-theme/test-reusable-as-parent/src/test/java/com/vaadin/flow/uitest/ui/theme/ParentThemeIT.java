/**
 * Copyright (C) 2024 Vaadin Ltd
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

import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.FONTAWESOME_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.MY_POLYMER_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.SUB_COMPONENT_ID;

public class ParentThemeIT extends ChromeBrowserTest {

    @Test
    public void childTheme_overridesParentTheme() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));

        Assert.assertEquals(
                "url(\"" + getRootURL()
                        + "/VAADIN/static/themes/child-theme/bg.jpg\")",
                body.getCssValue("background-image"));

        Assert.assertEquals("\"IBM Plex Mono\"",
                body.getCssValue("font-family"));

        Assert.assertEquals("Child should override parent external.",
                "url(\"" + getRootURL()
                        + "/VAADIN/static/themes/child-theme/img/gobo.png\")",
                $(SpanElement.class).id(BUTTERFLY_ID)
                        .getCssValue("background-image"));

        Assert.assertEquals("Child img selector should be used",
                "url(\"" + getRootURL()
                        + "/VAADIN/static/themes/child-theme/img/viking.png\")",
                $(SpanElement.class).id(OCTOPUSS_ID)
                        .getCssValue("background-image"));
    }

    @Test
    public void componentThemeIsApplied_childThemeTextColorIsApplied() {
        open();
        TestBenchElement myField = $(TestBenchElement.class).id(MY_POLYMER_ID);
        TestBenchElement input = myField.$(TestBenchElement.class)
                .id("vaadin-text-field-input-0");
        Assert.assertEquals("Polymer text field should have red background",
                "rgba(255, 0, 0, 1)", input.getCssValue("background-color"));

        Assert.assertEquals("Text field should have color as green",
                "rgba(0, 128, 0, 1)", input.getCssValue("color"));

    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "");
    }
}
