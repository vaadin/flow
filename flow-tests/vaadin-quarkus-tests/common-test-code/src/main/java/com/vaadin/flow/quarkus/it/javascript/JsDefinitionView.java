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
package com.vaadin.flow.quarkus.it.javascript;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;

/**
 * Runs JavaScript from a definition of the application.
 * {@code Element.executeJs(Class)} implements the definition with a JDK dynamic
 * proxy, which a native image has only when it is registered at build time.
 */
@Route("js-definition")
public class JsDefinitionView extends Div {

    public static final String RUN_ID = "run";
    public static final String RESULT_ID = "result";

    @JsDefinition
    public interface JoinJs extends Serializable {

        @JsExpression("return [$0, ...$1].join('-')")
        PendingJavaScriptResult join(String first, Object... rest);
    }

    public JsDefinitionView() {
        Span result = new Span();
        result.setId(RESULT_ID);
        NativeButton run = new NativeButton("Run the definition",
                event -> getElement().executeJs(JoinJs.class).join("a", 1, true)
                        .then(String.class, result::setText));
        run.setId(RUN_ID);
        add(run, result);
    }
}
