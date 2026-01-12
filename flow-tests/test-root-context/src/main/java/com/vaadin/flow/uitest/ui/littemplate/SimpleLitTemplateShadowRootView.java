/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Tag("simple-lit-template-shadow-root")
@JsModule("./lit-templates/simple-lit-template-shadow-root.js")
@Route(value = "com.vaadin.flow.uitest.ui.littemplate.SimpleLitTemplateShadowRootView", layout = ViewTestLayout.class)
public class SimpleLitTemplateShadowRootView extends LitTemplate {

    @Id
    public NativeButton mappedButton;
    @Id
    public Div label;
    @Id
    public Div sort;

    public SimpleLitTemplateShadowRootView() {
        getElement().setProperty("text", "Client button");
        mappedButton.setText("Server button");
        mappedButton.addClickListener(e -> {
            label.setText("Hello from server component event listener");
        });
        sort.setText("Sort");

    }

    @ClientCallable
    public void sayHello() {
        label.setText("Hello from ClientCallable");
    }

}
