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
package com.vaadin.flow.test.cspstrict.js;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;

/**
 * Runs JavaScript on an element through a {@link JsDefinition}: the expression
 * is in the bundle, so the browser compiles nothing.
 */
@Route("declared-js")
public class DeclaredJsView extends Div {

    @JsDefinition
    interface StatusJs extends Serializable {

        @JsExpression("this.textContent = $0;")
        void setStatus(String status);
    }

    public DeclaredJsView() {
        Span status = new Span("waiting");
        status.setId("status");
        add(status);
        status.getElement().executeJs(StatusJs.class)
                .setStatus("declared JavaScript ran");
    }
}
