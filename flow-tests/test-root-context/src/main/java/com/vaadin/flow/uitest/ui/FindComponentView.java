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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.FindComponentView", layout = ViewTestLayout.class)
public class FindComponentView extends Div {

    private Span span;
    private Div div2;
    private Div div1;

    public FindComponentView() {
        div1 = new Div();
        div1.setId("div1");
        div1.setText("Div 1");
        div2 = new Div();
        div2.setId("div2");
        div2.setText("Div 2");
        span = new Span("Node id");
        span.setId("nodeIdText");

        Input nodeIdInput = new Input();
        nodeIdInput.setId("nodeId");

        Div result = new Div();
        result.setId("result");

        NativeButton button = new NativeButton("Check", e -> {
            int uiId = getUI().get().getUIId();
            int nodeId = Integer.parseInt(nodeIdInput.getValue());
            VaadinSession session = VaadinSession.getCurrent();
            Element element = session.findElement(uiId, nodeId);
            result.setText("Found component with id "
                    + element.getComponent().get().getId().get());
        });
        button.setId("check");

        add(div1, div2, span, nodeIdInput, button, result);
    }

}
