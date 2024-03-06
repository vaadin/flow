/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.ElementRemoveItselfView", layout = ViewTestLayout.class)
public class ElementRemoveItselfView extends AbstractDivView {

    private Element layout = new Element("div");
    private Element button = new Element("button");

    public ElementRemoveItselfView() {
        button.setText("Remove me");
        button.setAttribute("id", "remove-me");

        layout.appendChild(button);
        button.addEventListener("click", evt -> {
            layout.removeAllChildren();
            Label label = new Label("All removed!");
            label.setId("all-removed");
            add(label);
        });
        getElement().appendChild(layout);
    }
}
