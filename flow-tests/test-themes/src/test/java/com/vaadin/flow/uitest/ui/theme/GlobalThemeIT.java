/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.List;
import java.util.stream.Collectors;

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
import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_LIT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_POLYMER_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.OCTOPUSS_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SNOWFLAKE_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SUB_COMPONENT_ID;

public class GlobalThemeIT extends ChromeBrowserTest {

    @Test
    public void noThemeAnnotation_customThemeIsApplied() {
        open();
        checkLogsForErrors();

        Assert.assertEquals(
            "Imported FontAwesome css file should be applied.",
            "\"Font Awesome 5 Free\"", $(SpanElement.class).id(FONTAWESOME_ID)
                        .getCssValue("font-family"));

        String iconUnicode = getCssPseudoElementValue(FONTAWESOME_ID,
                                          "::before");
        Assert.assertEquals(
           "Font-Icon from FontAwesome css file should be applied.",
           "\"\uf0f4\"", iconUnicode);

        getDriver().get(getRootURL() +
                "/path/VAADIN/static/@fortawesome/fontawesome-free/webfonts/fa-solid-900.svg");
        Assert.assertFalse("Font resource should be available",
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
        String script = "return window.getComputedStyle(" +
                                    "document.getElementById(arguments[0])" +
                                ", arguments[1]).content";
        JavascriptExecutor js = (JavascriptExecutor)driver;
        return (String) js.executeScript(script, elementId, pseudoElement);
    }
}
