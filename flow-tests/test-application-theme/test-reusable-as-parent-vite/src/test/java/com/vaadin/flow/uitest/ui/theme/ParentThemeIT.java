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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.BUTTERFLY_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.KEYBOARD_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.LEMON_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.MY_POLYMER_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ParentThemeView.SUN_ID;

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

    @Test
    public void childTheme_overridesParentTheme() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));

        Assert.assertEquals("\"IBM Plex Mono\"",
                body.getCssValue("font-family"));

        Matcher bodyImageMatcher = BODY_IMAGE_PATTERN
                .matcher(body.getCssValue("background-image"));
        Assert.assertTrue("Should override the body background image",
                bodyImageMatcher.matches());

        Matcher butterflyImageMatcher = BUTTERFLY_IMAGE_PATTERN
                .matcher($(SpanElement.class).id(BUTTERFLY_ID)
                        .getCssValue("background-image"));
        Assert.assertTrue("Should override the butterfly background image",
                butterflyImageMatcher.matches());

        Matcher octupussImageMatcher = OCTOPUSS_IMAGE_PATTERN
                .matcher($(SpanElement.class).id(OCTOPUSS_ID)
                        .getCssValue("background-image"));
        Assert.assertTrue("Should override the octupuss background image",
                octupussImageMatcher.matches());
    }

    @Test
    public void componentThemeIsApplied_childThemeTextColorIsApplied() {
        open();
        TestBenchElement myField = $(TestBenchElement.class).id(MY_POLYMER_ID);
        TestBenchElement input = myField.$("vaadin-input-container")
                .attribute("part", "input-field").first();
        Assert.assertEquals("Polymer text field should have red background",
                "rgba(255, 0, 0, 1)", input.getCssValue("background-color"));

    }

    @Test
    public void childTheme_cssAndAssetFromParentThemeAreApplied() {
        open();
        String imageUrl = $(DivElement.class).id(KEYBOARD_ID)
                .getCssValue("background-image");
        Assert.assertTrue(imageUrl.contains(getRootURL()
                + "/VAADIN/static/themes/reusable-theme/fortawesome/icons/keyboard.svg"));

        imageUrl = $(DivElement.class).id(LEMON_ID)
                .getCssValue("background-image");
        Assert.assertTrue(imageUrl.contains(getRootURL()
                + "/VAADIN/static/themes/reusable-theme/fortawesome/icons/lemon.svg"));

        imageUrl = $(DivElement.class).id(SUN_ID)
                .getCssValue("background-image");
        Assert.assertTrue(imageUrl.contains(getRootURL()
                + "/VAADIN/static/themes/reusable-theme/fortawesome/icons/sun.svg"));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "");
    }
}
