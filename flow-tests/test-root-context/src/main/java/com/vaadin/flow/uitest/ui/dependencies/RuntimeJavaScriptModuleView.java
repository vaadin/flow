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

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Bare relative {@code @JavaScript} values with {@code type = MODULE} are not
 * bundled but served from the static web resources at runtime, and end up in
 * the page as {@code <script type="module">} elements.
 */
@JavaScript(value = "dependencies/runtime-module.js", type = JavaScript.Type.MODULE)
@JavaScript(value = "dependencies/runtime-module-lazy.js", type = JavaScript.Type.MODULE, loadMode = LoadMode.LAZY)
@Route(value = "com.vaadin.flow.uitest.ui.dependencies.RuntimeJavaScriptModuleView", layout = ViewTestLayout.class)
public class RuntimeJavaScriptModuleView extends Div {

    public RuntimeJavaScriptModuleView() {
        Div content = new Div("A view with runtime ES module dependencies");
        content.setId("view-content");
        add(content);
    }
}
