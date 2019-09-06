/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

import elemental.json.Json;

@Route(value = "com.vaadin.flow.uitest.ui.ExecJavaScriptView", layout = ViewTestLayout.class)
public class ExecJavaScriptView extends AbstractDivView {
    @Override
    protected void onShow() {
        NativeButton alertButton = createJsButton("Alert", "alertButton",
                "window.alert($0)", "Hello world");
        NativeButton focusButton = createJsButton("Focus Alert button",
                "focusButton", "$0.focus()", alertButton);
        NativeButton swapText = createJsButton("Swap button texts",
                "swapButton",
                "(function() {var t = $0.textContent; $0.textContent = $1.textContent; $1.textContent = t;})()",
                alertButton, focusButton);
        NativeButton logButton = createJsButton("Log", "logButton",
                "console.log($0)", JsonUtils.createArray(
                        Json.create("Hello world"), Json.create(true)));

        NativeButton createElementButton = createButton(
                "Create and update element", "createButton", e -> {
                    Input input = new Input();
                    input.addClassName("newInput");
                    input.getElement().executeJs("this.value=$0",
                            "Value from js");
                    add(input);
                });

        add(alertButton, focusButton, swapText, logButton, createElementButton);
    }

    private NativeButton createJsButton(String text, String id, String script,
            Serializable... arguments) {
        return createButton(text, id,
                e -> UI.getCurrent().getPage().executeJs(script, arguments));
    }
}
