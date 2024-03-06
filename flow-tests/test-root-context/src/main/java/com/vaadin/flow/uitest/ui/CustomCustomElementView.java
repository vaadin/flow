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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.CustomCustomElementView", layout = ViewTestLayout.class)
@JsModule("./com/vaadin/flow/uitest/ui/CustomCustomElement.js")
public class CustomCustomElementView extends AbstractDivView {

    public CustomCustomElementView() {
        Element element = new Element("custom-custom-element");
        element.getStyle().set("display", "block");

        element.setProperty("property", "initial");

        getElement().appendChild(element);

        add(new NativeButton("update property",
                event -> element.setProperty("property", "updated")));
    }

}
