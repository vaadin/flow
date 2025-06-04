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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.webcomponent.ThemedComponent.EMBEDDED_ID;
import static com.vaadin.flow.webcomponent.ThemedComponent.HAND_ID;
import static com.vaadin.flow.webcomponent.ThemedComponent.MY_COMPONENT_ID;

public class ApplicationThemeComponentIT extends ChromeBrowserTest {

    private static final String FIND_FONT_FACE_RULE_SCRIPT =
    //@formatter:off
        "let found = false;"
        + "for (let as = 0; as < target.adoptedStyleSheets.length; ++as) {"
        + "    let styleSheetRules = target.adoptedStyleSheets[as].rules;"
        + "    for (var ss = 0; ss < styleSheetRules.length; ++ss) {"
        + "        let cssRule = styleSheetRules[ss];"
        + "        if (cssRule instanceof CSSFontFaceRule && cssRule.cssText.startsWith(\"@font-face { font-family: Ostrich;\")) {"
        + "            found = true;"
        + "        }"
        + "    }"
        + "}"
        + "return found;";
    //@formatter:on

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void applicationTheme_GlobalCss_isUsedOnlyInEmbeddeComponent() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        validateEmbeddedComponent($("themed-component").id("first"), "first");
        validateEmbeddedComponent($("themed-component").id("second"), "second");

        final WebElement body = findElement(By.tagName("body"));
        // With Vite assets are served from /VAADIN/build in production mode
        String imageUrl = body.getCssValue("background-image");
        Assert.assertFalse("background-image should not be applied to body",
                imageUrl.matches("url\\(\"" + getRootURL()
                        + "/VAADIN/build/bg.+\\.jpg\"\\)"));

        Assert.assertNotEquals("Ostrich", body.getCssValue("font-family"));

        Assert.assertEquals(
                "Embedded style should not match external component",
                "rgba(0, 0, 255, 1)",
                $(SpanElement.class).id("overflow").getCssValue("color"));
        getDriver().get(getRootURL() + "/themes/reusable-theme/img/bg.jpg");
        Assert.assertFalse("app-theme background file should be served",
                driver.getPageSource().contains("Could not navigate"));
    }

    private void validateEmbeddedComponent(TestBenchElement themedComponent,
            String target) {
        // With Vite assets are served from /VAADIN/build in production mode
        String imageUrl = themedComponent.getCssValue("background-image");
        Assert.assertTrue(target + " didn't contain the background image",
                imageUrl.matches("url\\(\"" + getRootURL()
                        + "/VAADIN/build/bg.+\\.jpg\"\\)"));

        Assert.assertEquals(target + " didn't contain font-family", "Ostrich",
                themedComponent.getCssValue("font-family"));

        final TestBenchElement embeddedComponent = themedComponent
                .$(DivElement.class).id(EMBEDDED_ID);

        final SpanElement handElement = embeddedComponent.$(SpanElement.class)
                .id(HAND_ID);

        Assert.assertEquals("Color should have been applied",
                "rgba(0, 128, 0, 1)", handElement.getCssValue("color"));
    }

    @Test
    public void componentThemeIsApplied() {
        open();

        final TestBenchElement themedComponent = $("themed-component").first();
        final TestBenchElement embeddedComponent = themedComponent
                .$(DivElement.class).id(EMBEDDED_ID);

        TestBenchElement myField = embeddedComponent.$(TestBenchElement.class)
                .id(MY_COMPONENT_ID);
        TestBenchElement input = myField.$("vaadin-input-container")
                .withAttribute("part", "input-field").first();
        Assert.assertEquals("Polymer text field should have red background",
                "rgba(255, 0, 0, 1)", input.getCssValue("background-color"));
    }

    @Test
    public void documentCssFonts_fontsAreAppliedAndAvailable() {
        open();
        checkLogsForErrors();
        final TestBenchElement themedComponent = $("themed-component").first();
        final TestBenchElement embeddedComponent = themedComponent
                .$(DivElement.class).id(EMBEDDED_ID);

        final SpanElement handElement = embeddedComponent.$(SpanElement.class)
                .id(HAND_ID);
        Assert.assertEquals("Font family faulty", "\"Font Awesome 5 Free\"",
                handElement.getCssValue("font-family"));
        Assert.assertEquals("Font weight faulty", "900",
                handElement.getCssValue("font-weight"));
        Assert.assertEquals("display value faulty", "inline-block",
                handElement.getCssValue("display"));

        getDriver().get(getRootURL()
                + "/path/VAADIN/static/@fortawesome/fontawesome-free/webfonts/fa-solid-900.woff2");
        Assert.assertFalse("Font resource should be available",
                driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    public void documentCssFonts_fromLocalCssFile_fontAppliedToDocumentRoot() {
        open();

        Object ostrichFontStylesFound = getCommandExecutor().executeScript(
                "let target = document;" + FIND_FONT_FACE_RULE_SCRIPT);

        Assert.assertTrue(
                "Expected Ostrich font to be applied to document root element",
                (Boolean) ostrichFontStylesFound);
    }

    @Test
    public void documentCssFonts_fromLocalCssFile_fontNotAppliedToEmbeddedComponent() {
        open();

        Object ostrichFontStylesFoundForEmbedded = getCommandExecutor()
                .executeScript("let target = document.getElementsByTagName"
                        + "('themed-component')[0].shadowRoot;"
                        + FIND_FONT_FACE_RULE_SCRIPT);

        Assert.assertFalse(
                "Expected no Ostrich font to be applied to embedded component",
                (Boolean) ostrichFontStylesFoundForEmbedded);
    }
}
