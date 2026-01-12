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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ExporterIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/embedded.html";
    }

    @Test
    public void embeddedWebComponent_cssImportStyles_areAppliedInsideComponent() {
        open();
        checkLogsForErrors();

        waitForElementVisible(By.id("web-component"));

        TestBenchElement webComponent = $("login-form").id("web-component");
        Assert.assertNotNull("login-form web component should be present",
                webComponent);

        // Access shadow DOM to find the internal inputs
        TestBenchElement userNameInput = webComponent.$("input").id("userName");
        TestBenchElement passwordInput = webComponent.$("input").id("password");

        // Verify that the styles from @CssImport ARE applied inside the web
        // component
        Assert.assertEquals(
                "Internal userName input should have blue background from @CssImport",
                "rgba(0, 0, 255, 1)",
                userNameInput.getCssValue("background-color"));

        Assert.assertEquals(
                "Internal password input should have red background from @CssImport",
                "rgba(255, 0, 0, 1)",
                passwordInput.getCssValue("background-color"));
    }

    @Test
    public void embeddedWebComponent_cssImportStyles_doNotLeakToExternalElements() {
        open();
        checkLogsForErrors();

        TestBenchElement webComponent = $("login-form").id("web-component");
        Assert.assertNotNull("login-form web component should be present",
                webComponent);

        // Get the external input fields that have the same class names
        WebElement externalUserNameInput = findElement(By.id("external-input"));
        WebElement externalPasswordInput = findElement(
                By.id("external-password"));

        // Verify that external inputs do NOT have the colored backgrounds
        // The styles should be contained within the web component's shadow DOM
        String externalUserNameBg = externalUserNameInput
                .getCssValue("background-color");
        String externalPasswordBg = externalPasswordInput
                .getCssValue("background-color");

        // The external inputs should not have blue/red backgrounds
        Assert.assertNotEquals(
                "External input with .userName class should not have blue background from web component @CssImport",
                "rgba(0, 0, 255, 1)", externalUserNameBg);

        Assert.assertNotEquals(
                "External input with .password class should not have red background from web component @CssImport",
                "rgba(255, 0, 0, 1)", externalPasswordBg);

    }
}
