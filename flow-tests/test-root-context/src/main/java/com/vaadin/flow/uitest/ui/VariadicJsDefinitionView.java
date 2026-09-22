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

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Calls a JavaScript definition whose last parameter collects the arguments
 * that follow the fixed ones, which the browser gets as a rest parameter of the
 * generated function.
 */
@Route(value = "com.vaadin.flow.uitest.ui.VariadicJsDefinitionView", layout = ViewTestLayout.class)
public class VariadicJsDefinitionView extends AbstractDivView {

    /**
     * Joins what it is called with, so that what the browser received of a call
     * is what the server reads back.
     */
    @JsDefinition
    public interface JoinJs extends Serializable {

        @JsExpression("return [$0, ...$1].join('-')")
        PendingJavaScriptResult join(String first, Object... rest);
    }

    public VariadicJsDefinitionView() {
        Div result = new Div();
        result.setId("result");
        add(result);

        add(createButton("Call with trailing arguments", "call-many",
                event -> getElement().executeJs(JoinJs.class)
                        .join("a", 1, true, "b")
                        .then(String.class, result::setText)));

        add(createButton("Call with none", "call-none",
                event -> getElement().executeJs(JoinJs.class).join("a")
                        .then(String.class, result::setText)));
    }
}
