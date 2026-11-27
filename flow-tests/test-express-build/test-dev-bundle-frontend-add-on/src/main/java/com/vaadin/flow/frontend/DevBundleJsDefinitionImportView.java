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
package com.vaadin.flow.frontend;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Calls JavaScript that imports a frontend file of the project and one of a jar
 * through {@link JsDefinitionImport}, which the dev bundle carries and hashes
 * like a {@code @JsModule} file.
 */
@Route("com.vaadin.flow.frontend.DevBundleJsDefinitionImportView")
public class DevBundleJsDefinitionImportView extends Div {
    static final String STATUS_ID = "definition-import-status";
    static final String RUN_ID = "definition-import-run";

    @JsDefinition
    @JsDefinitionImport(from = "./js/definition-import.js", exports = "describe")
    @JsDefinitionImport(from = "./definition-import-addon.js", namespace = "addon")
    interface DescribingJs extends JsInvokers {
        @JsExpression("this.textContent = addon.describe(describe($0));")
        void describe(String text);
    }

    public DevBundleJsDefinitionImportView() {
        Span status = new Span("waiting");
        status.setId(STATUS_ID);
        NativeButton run = new NativeButton("Run", event -> status.getElement()
                .executeJs(DescribingJs.class).describe("imported"));
        run.setId(RUN_ID);
        add(run, status);
    }
}
