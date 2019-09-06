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

package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@JavaScript(ComponentWithExternalJavaScript.SOME_RANDOM_EXTERNAL_JS_URL)
@JavaScript(ComponentWithExternalJavaScript.SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL)
@Route(value = "com.vaadin.flow.uitest.ui.dependencies.ExternalJavaScriptView", layout = ViewTestLayout.class)
public class ExternalJavaScriptView extends Div {

    public ExternalJavaScriptView() {
        NativeButton button = new NativeButton("Add component", event -> {
            ComponentWithExternalJavaScript component = new ComponentWithExternalJavaScript();
            component.setId("componentWithExternalJavaScript");
            add(component);
        });
        button.setId("addComponentButton");
        add(button);
    }
}
