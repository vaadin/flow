/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DomListenerOnAttachView", layout = ViewTestLayout.class)
@JsModule("./DomListenerOnAttach.js")
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
