/*
 * Copyright 2000-2021 Vaadin Ltd.
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
        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
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
