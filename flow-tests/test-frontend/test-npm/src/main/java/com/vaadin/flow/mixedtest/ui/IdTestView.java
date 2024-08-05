/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.mixedtest.ui;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;

@Route(value = "route-path")
@Tag("my-component")
@JsModule("./my-component.js")
public class IdTestView extends LitTemplate {

    @WebServlet("/servlet-path/*")
    public static class MyServlet extends VaadinServlet {
    }

    @Id
    NativeButton button;
    @Id
    Div content;

    public IdTestView() {
        button.addClickListener(e -> {
            String s = content.getText();
            Integer val = 1
                    + Integer.parseInt(s == null || s.isEmpty() ? "0" : s);
            content.setText(String.valueOf(val));
        });
    }
}
