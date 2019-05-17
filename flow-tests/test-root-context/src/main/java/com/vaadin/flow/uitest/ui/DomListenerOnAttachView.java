/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DomListenerOnAttachView", layout = ViewTestLayout.class)
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/DomListenerOnAttach.html")
@JsModule("DomListenerOnAttach.js")
public class DomListenerOnAttachView extends AbstractDivView {
    public DomListenerOnAttachView() {
        Div status = new Div();
        status.setText("Waiting for event");
        status.setId("status");

        Element element = new Element("event-on-attach");
        element.addEventListener("attach", event -> {
            status.setText("Event received");
        });

        getElement().appendChild(element, status.getElement());
    }
}
