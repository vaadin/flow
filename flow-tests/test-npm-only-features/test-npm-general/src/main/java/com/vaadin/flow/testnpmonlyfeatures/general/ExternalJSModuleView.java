/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.testnpmonlyfeatures.general;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@JsModule(ExternalJSModuleView.SOME_RANDOM_EXTERNAL_JS_MODULE_URL)
@Route(value = "com.vaadin.flow.testnpmonlyfeatures.general.ExternalJSModuleView", layout = ViewTestLayout.class)
public class ExternalJSModuleView extends Div {
    public static final String SOME_RANDOM_EXTERNAL_JS_MODULE_URL = "https://some-external-website.fi/some-js-module.js";

    public ExternalJSModuleView() {
        NativeButton button = new NativeButton("Add component", event -> {
            ComponentWithExternalJsModule component = new ComponentWithExternalJsModule();
            component.setId("componentWithExternalJsModule");
            add(component);
        });
        button.setId("addComponentButton");
        add(button);
    }
}
