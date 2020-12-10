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
import static com.vaadin.flow.webcomponent.ThemedComponent.MY_LIT_ID;
import static com.vaadin.flow.webcomponent.ThemedComponent.MY_POLYMER_ID;

public class ApplicationThemeComponentIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void applicationTheme_GlobalCss_isUsedOnlyInEmbeddeComponent() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final TestBenchElement themedComponent = $("themed-component").first();

        Assert.assertEquals(
            "url(\"" + getRootURL() + "/VAADIN/static/theme/embedded-theme/img/bg.jpg\")",
            themedComponent.getCssValue("background-image"));

        Assert.assertEquals("Ostrich",
            themedComponent.getCssValue("font-family"));

        final WebElement body = findElement(By.tagName("body"));
        Assert.assertNotEquals(
            "url(\"" + getRootURL() + "/path/VAADIN/static/theme/embedded-theme/img/bg.jpg\")",
            body.getCssValue("background-image"));

        Assert.assertNotEquals("Ostrich", body.getCssValue("font-family"));

        getDriver().get(getRootURL() + "/VAADIN/static/theme/embedded-theme/img/bg.jpg");
        Assert.assertFalse("app-theme background file should be served",
            driver.getPageSource().contains("Could not navigate"));
    }

    @Test
    public void componentThemeIsApplied_forPolymerAndLit() {
        open();

        final TestBenchElement themedComponent = $("themed-component").first();
        final TestBenchElement embeddedComponent = themedComponent
            .$(DivElement.class).id(EMBEDDED_ID);

        TestBenchElement myField = embeddedComponent.$(TestBenchElement.class)
            .id(MY_POLYMER_ID);
        TestBenchElement input = myField.$(TestBenchElement.class)
            .id("vaadin-text-field-input-0");
        Assert.assertEquals("Polymer text field should have red background",
            "rgba(255, 0, 0, 1)", input.getCssValue("background-color"));

        myField = embeddedComponent.$(TestBenchElement.class).id(MY_LIT_ID);
        final SpanElement radio = myField.$(SpanElement.class).all().stream()
            .filter(element -> "radio".equals(element.getAttribute("part")))
            .findFirst().orElseGet(null);

        Assert.assertNotNull("Element with part='radio' was not found", radio);

        Assert.assertEquals("Lit radiobutton should have red background",
            "rgba(255, 0, 0, 1)", radio.getCssValue("background-color"));
    }
}
