/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.Serializable;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.html.Input;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.ui.UI;

import elemental.json.Json;

@Route(value = "com.vaadin.flow.uitest.ui.ExecJavaScriptView", layout = ViewTestLayout.class)
public class ExecJavaScriptView extends AbstractDivView {
    @Override
    protected void onShow() {
        NativeButton alertButton = createJsButton("Alert", "alertButton",
                "window.alert($0)", "Hello world");
        NativeButton focusButton = createJsButton("Focus Alert button", "focusButton",
                "$0.focus()", alertButton);
        NativeButton swapText = createJsButton("Swap button texts", "swapButton",
                "(function() {var t = $0.textContent; $0.textContent = $1.textContent; $1.textContent = t;})()",
                alertButton, focusButton);
        NativeButton logButton = createJsButton("Log", "logButton", "console.log($0)",
                JsonUtils.createArray(Json.create("Hello world"),
                        Json.create(true)));

        NativeButton createElementButton = new NativeButton("Create and update element",
                e -> {
                    Input input = new Input();
                    input.addClassName("newInput");
                    UI.getCurrent().getPage().executeJavaScript("$0.value = $1",
                            input, "Value from js");
                    add(input);
                });
        createElementButton.setId("createButton");

        add(alertButton, focusButton, swapText, logButton, createElementButton);
    }

    private NativeButton createJsButton(String text, String id, String script,
            Serializable... arguments) {
        NativeButton button = new NativeButton(text, e -> UI.getCurrent().getPage()
                .executeJavaScript(script, arguments));

        button.setId(id);
        return button;
    }
}
