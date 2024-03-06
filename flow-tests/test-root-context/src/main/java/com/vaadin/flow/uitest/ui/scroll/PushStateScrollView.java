/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import java.util.function.BiConsumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.router.Route;

import elemental.json.JsonValue;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.PushStateScrollView", layout = ViewTestLayout.class)
public class PushStateScrollView extends AbstractDivView {
    public PushStateScrollView() {
        Element filler = ElementFactory.createDiv(
                "Pushing or replacing history state should not affect the scroll position. Scroll down for buttons to click.");
        filler.getStyle().set("height", "150vh");

        History history = UI.getCurrent().getPage().getHistory();

        getElement().appendChild(filler,
                createButton("push", history::pushState),
                createButton("replace", history::replaceState));
    }

    private static Element createButton(String name,
            BiConsumer<JsonValue, String> action) {
        String location = PushStateScrollView.class.getName() + "/" + name;

        Element button = ElementFactory.createButton(name);

        button.setAttribute("id", name);
        button.addEventListener("click", e -> action.accept(null, location));

        return button;
    }
}
