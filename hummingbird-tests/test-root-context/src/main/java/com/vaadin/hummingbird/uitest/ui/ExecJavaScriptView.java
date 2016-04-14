/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Input;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.ui.UI;

import elemental.json.Json;

public class ExecJavaScriptView extends AbstractDivView {
    @Override
    protected void onShow() {
        Button alertButton = createJsButton("Alert", "alertButton",
                "window.alert($0)", "Hello world");
        Button focusButton = createJsButton("Focus Alert button", "focusButton",
                "$0.focus()", alertButton);
        Button swapText = createJsButton("Swap button texts", "swapButton",
                "(function() {var t = $0.textContent; $0.textContent = $1.textContent; $1.textContent = t;})()",
                alertButton, focusButton);
        Button logButton = createJsButton("Log", "logButton", "console.log($0)",
                JsonUtil.createArray(Json.create("Hello world"),
                        Json.create(true)));

        Button createElementButton = new Button("Create and update element",
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

    private Button createJsButton(String text, String id, String script,
            Object... arguments) {
        Button button = new Button(text, e -> UI.getCurrent().getPage()
                .executeJavaScript(script, arguments));

        button.setId(id);
        return button;
    }
}
