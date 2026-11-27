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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Runs JavaScript that calls what a module exports, imported through
 * {@link JsDefinitionImport}: the module is in a chunk of the bundle that is
 * loaded the first time the definition is called.
 */
@Route("declared-js-import")
public class DeclaredJsImportView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./js-definition-import.js", defaultExport = "describe", exports = "shout")
    @JsDefinitionImport(from = "./js-definition-import.js", namespace = "module")
    interface ImportingStatusJs extends JsInvokers {

        @JsExpression("this.textContent = shout(describe($0)) + ' ' + (module.shout === shout);")
        void setStatus(String status);

        // Hides an imported name. Only declared: a clash with the import
        // would stop the whole file of definitions from parsing.
        @JsExpression("const module = this; module.dataset.shadowed = 'true';")
        void markShadowed();
    }

    public DeclaredJsImportView() {
        Span status = new Span("waiting");
        status.setId("status");
        NativeButton run = new NativeButton("Run", event -> status.getElement()
                .executeJs(ImportingStatusJs.class).setStatus("imported"));
        run.setId("run");
        add(run, status);
    }
}
