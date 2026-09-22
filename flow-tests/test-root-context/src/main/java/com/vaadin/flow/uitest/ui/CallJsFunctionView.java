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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Calls functions of a browser object through
 * {@link com.vaadin.flow.dom.Element#callJsFunction(String, Object...)}, which
 * the browser looks up on the element and calls.
 */
@Route(value = "com.vaadin.flow.uitest.ui.CallJsFunctionView", layout = ViewTestLayout.class)
public class CallJsFunctionView extends AbstractDivView {

    public CallJsFunctionView() {
        Div result = new Div();
        result.setId("result");

        Div target = new Div();
        target.setId("target");
        // Two functions to call: one on the element, and one on a property of
        // it, which is what a connector of a component is
        target.getElement().executeJs("""
                this.join = function () {
                    return [...arguments].join('-');
                };
                this.$connector = {
                    label: 'connector',
                    describe(suffix) {
                        return this.label + suffix;
                    }
                };
                """);

        add(target, result);

        add(createButton("Call with arguments", "call", event -> target
                .getElement().callJsFunction("join", "a", 1, true)
                .then(String.class, result::setText)));

        add(createButton("Call through a property", "call-on-property",
                event -> target.getElement()
                        .callJsFunction("$connector.describe", "!")
                        .then(String.class, result::setText)));

        add(createButton("Call what is not there", "call-missing",
                event -> target.getElement().callJsFunction("missing").then(
                        value -> result.setText("resolved: " + value),
                        error -> result.setText("failed"))));
    }
}
