/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.trigger.JsAction;
import com.vaadin.flow.component.trigger.JsOutput;
import com.vaadin.flow.component.trigger.JsTrigger;
import com.vaadin.flow.component.trigger.Output;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Validates the JS escape hatch: a {@link JsTrigger} wires a click listener on
 * a button; a {@link JsAction} reads from a {@link JsOutput} and writes the
 * produced value to a {@code <span>}. No custom Java action class and no custom
 * client TS module — purely the {@code flow:js} dispatcher.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerJsEscapeHatchView", layout = ViewTestLayout.class)
public class TriggerJsEscapeHatchView extends AbstractDivView {

    @Override
    protected void onShow() {
        NativeButton button = new NativeButton("Run JS");
        button.setId("run");

        Span result = new Span("(initial)");
        result.setId("result");

        add(button, result);

        Output<String> message = new JsOutput<>(String.class,
                "return 'js-escape-hatch';");
        new JsTrigger(button,
                "this.addEventListener('click', () => trigger());")
                .triggers(new JsAction(
                        "document.getElementById('result').textContent = output(0);",
                        message));
    }
}
