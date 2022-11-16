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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.MY_POLYMER_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.OCTOPUSS_ID;

public class ParentThemeIT extends ChromeBrowserTest {
    private final Pattern BODY_IMAGE_PATTERN = Pattern.compile(
            ("url\\(\"" + getRootURL() + "/VAADIN/build/bg(.*)\\.jpg\"\\)")
                    .replaceAll("/", "\\\\/"));

    private final Pattern BUTTERFLY_IMAGE_PATTERN = Pattern.compile(
            ("url\\(\"" + getRootURL() + "/VAADIN/build/gobo(.*)\\.png\"\\)")
                    .replaceAll("/", "\\\\/"));

    private final Pattern OCTOPUSS_IMAGE_PATTERN = Pattern.compile(
            ("url\\(\"" + getRootURL() + "/VAADIN/build/viking(.*)\\.png\"\\)")
                    .replaceAll("/", "\\\\/"));

    @BrowserTest
    public void childTheme_overridesParentTheme() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));

        Assertions.assertEquals("\"IBM Plex Mono\"",
                body.getCssValue("font-family"));

        Matcher bodyImageMatcher = BODY_IMAGE_PATTERN
                .matcher(body.getCssValue("background-image"));
        Assertions.assertTrue(bodyImageMatcher.matches(),
                "Should override the body background image");

        Matcher butterflyImageMatcher = BUTTERFLY_IMAGE_PATTERN
                .matcher($(SpanElement.class).id(BUTTERFLY_ID)
                        .getCssValue("background-image"));
        Assertions.assertTrue(butterflyImageMatcher.matches(),
                "Should override the butterfly background image");

        Matcher octupussImageMatcher = OCTOPUSS_IMAGE_PATTERN
                .matcher($(SpanElement.class).id(OCTOPUSS_ID)
                        .getCssValue("background-image"));
        Assertions.assertTrue(octupussImageMatcher.matches(),
                "Should override the octupuss background image");
    }

    @BrowserTest
    public void componentThemeIsApplied_childThemeTextColorIsApplied() {
        open();
        TestBenchElement myField = $(TestBenchElement.class).id(MY_POLYMER_ID);
        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
        Assertions.assertEquals("rgba(255, 0, 0, 1)",
                input.getCssValue("background-color"),
                "Polymer text field should have red background");

        Assertions.assertEquals("rgba(0, 128, 0, 1)",
                input.getCssValue("color"),
                "Text field should have color as green");

    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "");
    }
}
