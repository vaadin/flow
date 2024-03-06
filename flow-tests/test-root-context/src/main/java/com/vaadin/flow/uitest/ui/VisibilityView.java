/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.VisibilityView", layout = ViewTestLayout.class)
public class VisibilityView extends AbstractDivView {

    public VisibilityView() {
        setId("main");

        Div div = new Div();
        div.setText("Target to make visible/invisible");
        div.setId("visibility");
        div.setVisible(false);

        Label label = new Label("Nested element");
        label.setId("nested-label");
        div.add(label);

        NativeButton updateVisibility = createButton("Update visibility",
                "updateVisibiity", event -> div.setVisible(!div.isVisible()));

        NativeButton updateLabelVisibility = createButton(
                "Update label visibility", "updateLabelVisibiity",
                event -> label.setVisible(!label.isVisible()));

        NativeButton updateStyle = createButton(
                "Update target element property", "updateProperty", event -> {
                    div.setClassName("foo");
                    label.setClassName("bar");
                });

        add(div, updateVisibility, updateStyle, updateLabelVisibility);
    }

}
