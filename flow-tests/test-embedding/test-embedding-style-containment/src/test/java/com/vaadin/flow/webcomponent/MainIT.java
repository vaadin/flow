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

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class MainIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/com.vaadin.flow.webcomponent.MainView";
    }

    @Test
    public void cssImportOnExporter_doesNotLeakToMainView() {
        open();
        checkLogsForErrors();

        InputTextElement userNameInput = $(InputTextElement.class)
                .id("userName");
        InputTextElement passwordInput = $(InputTextElement.class)
                .id("password");

        // The @CssImport on LoginFormExporter should NOT affect the regular
        // Vaadin view
        // Verify that the inputs do NOT have the colored backgrounds from
        // styles-in-frontend.css
        String userNameBg = userNameInput.getCssValue("background-color");
        String passwordBg = passwordInput.getCssValue("background-color");

        Assert.assertNotEquals(
                "LoginForm in MainView should not have blue background from @CssImport on exporter",
                "rgba(0, 0, 255, 1)", userNameBg);

        Assert.assertNotEquals(
                "LoginForm in MainView should not have red background from @CssImport on exporter",
                "rgba(255, 0, 0, 1)", passwordBg);
    }
}
