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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.ImageElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_LIT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_POLYMER_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeView.SNOWFLAKE_ID;

public class ThemeIT extends ChromeBrowserTest {

    @Test
    public void secondTheme_staticFilesNotCopied() {
        getDriver().get(getRootURL() + "/VAADIN/static/img/bg.jpg");
        Assert.assertFalse("app-theme static files should be copied",
            driver.getPageSource().contains("HTTP ERROR 404 Not Found"));

        getDriver().get(getRootURL() + "/VAADIN/static/no-copy.txt");
        Assert.assertTrue("no-copy theme should not be handled",
            driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    @Test
    public void applicationTheme_GlobalCss_isUsed() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));
        Assert.assertEquals(
            "url(\"" + getRootURL() + "/VAADIN/static/img/bg.jpg\")",
            body.getCssValue("background-image"));

        Assert.assertEquals("Ostrich", body.getCssValue("font-family"));

        getDriver().get(getRootURL() + "/VAADIN/static/img/bg.jpg");
        Assert.assertFalse("app-theme background file should be served",
            driver.getPageSource().contains("Could not navigate"));
    }

    @Test
    public void componentThemeIsApplied_forPolymerAndLit() {
        open();
        TestBenchElement myField = $(TestBenchElement.class).id(MY_POLYMER_ID);
        TestBenchElement input = myField.$(TestBenchElement.class)
            .id("vaadin-text-field-input-0");
        Assert.assertEquals("Polymer text field should have red background",
            "rgba(255, 0, 0, 1)", input.getCssValue("background-color"));

        myField = $(TestBenchElement.class).id(MY_LIT_ID);
        final SpanElement radio = myField.$(SpanElement.class).all().stream()
            .filter(element -> "radio".equals(element.getAttribute("part")))
            .findFirst().orElseGet(null);

        Assert.assertNotNull("Element with part='radio' was not found", radio);

        Assert.assertEquals("Lit radiobutton should have red background",
            "rgba(255, 0, 0, 1)", radio.getCssValue("background-color"));
    }

    @Test
    public void subCssWithRelativePath_urlPathIsNotRelative() {
        open();
        checkLogsForErrors();

        Assert.assertEquals("Imported css file URLs should have been handled.",
            "url(\"" + getRootURL() + "/VAADIN/static/icons/archive.png\")",
            $(SpanElement.class).id("sub-component")
                .getCssValue("background-image"));
    }

    @Test
    public void staticModuleAsset_servedFromStatic() {
        open();
        checkLogsForErrors();

        Assert.assertEquals(
            "Node assets should have been copied to 'VAADIN/static'",
            getRootURL() + "/VAADIN/static/fortawesome/icons/snowflake.svg",
            $(ImageElement.class).id(SNOWFLAKE_ID).getAttribute("src"));

        open(getRootURL() + "/" + $(ImageElement.class).id(SNOWFLAKE_ID)
            .getAttribute("src"));
        Assert.assertFalse("Node static icon should be available",
            driver.getPageSource().contains("HTTP ERROR 404 Not Found"));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.substring(view.length());
    }

}
