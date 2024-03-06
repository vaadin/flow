/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.stream.Stream;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ElementInitOrderView", layout = ViewTestLayout.class)
@JsModule("./ElementInitOrder.js")
public class ElementInitOrderView extends AbstractDivView {
    public ElementInitOrderView() {
        NativeButton reattach = createButton("Reattach components", "reattach",
                event -> reattachElements());

        add(reattach, new Html("<br />"));

        reattachElements();
    }

    private void reattachElements() {
        Stream.of("init-order-polymer", "init-order-nopolymer")
                // Remove old child if present
                .peek(name -> getElement().getChildren()
                        .filter(child -> child.getTag().equals(name))
                        .findFirst().ifPresent(Element::removeFromParent))
                // Create and attach new child
                .map(ElementInitOrderView::createElement)
                .forEach(getElement()::appendChild);
    }

    private static Element createElement(String tag) {
        Element element = new Element(tag);
        element.appendChild(new Element("span"));
        element.getStyle().set("animationName", "style");
        element.getClassList().add("class");
        element.setAttribute("attribute", "attribute");
        element.setProperty("property", "property");
        return element;
    }
}
