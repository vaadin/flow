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
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.JsImports;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Values exported by a JS module are made available to a server-sent JS
 * expression by declaring them with {@code @JsModule(imports = ...)} and
 * passing a {@link JsImports} reference as a parameter.
 */
@Route(value = "com.vaadin.flow.uitest.ui.dependencies.JsImportsView", layout = ViewTestLayout.class)
public class JsImportsView extends Div {

    @JsModule(value = "./js-imports-module.js", imports = { "setText",
            "marker" })
    static final class NamedImports {
    }

    @JsModule(value = "./js-imports-module.js", importAll = true)
    static final class NamespaceImports {
    }

    public JsImportsView() {
        Div named = new Div();
        named.setId("named");
        Div namespace = new Div();
        namespace.setId("namespace");
        Div deferred = new Div();
        deferred.setId("deferred");

        NativeButton runDeferred = new NativeButton("Run deferred",
                event -> deferred.getElement().executeJs(
                        "$0.setText(this, 'deferred ' + $0.marker)",
                        JsImports.of(NamedImports.class)));
        runDeferred.setId("run-deferred");

        add(named, namespace, deferred, runDeferred);

        // Both the imported function and the imported constant arrive through
        // the same parameter
        named.getElement().executeJs("$0.setText(this, 'named ' + $0.marker)",
                JsImports.of(NamedImports.class));

        // A whole namespace object, including the default export
        namespace.getElement().executeJs(
                "$0.default(this); $0.setText(this, 'namespace ' + $0.marker)",
                JsImports.of(NamespaceImports.class));
    }
}
