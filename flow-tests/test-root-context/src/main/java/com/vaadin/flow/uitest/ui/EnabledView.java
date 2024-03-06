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

@Route(value = "com.vaadin.flow.uitest.ui.EnabledView", layout = ViewTestLayout.class)
public class EnabledView extends AbstractDivView {

    public EnabledView() {
        setId("main");

        Div div = new Div();
        div.setText("Target to enable/disable");
        div.setId("enabled");
        div.getElement().setEnabled(false);

        Label label = new Label("Nested element");
        label.setId("nested-label");
        div.add(label);

        NativeButton updateStyle = createButton(
                "Update target element property", "updateProperty", event -> {
                    div.setClassName("foo");
                    label.setClassName("bar");
                });
        updateStyle.getElement().setEnabled(false);

        NativeButton updateEnableButton = createButton(
                "Change enable state for buttons", "enableButton", event -> {
                    updateStyle.getElement()
                            .setEnabled(!updateStyle.getElement().isEnabled());
                    updateStyle.setClassName("disabled",
                            !updateStyle.getElement().isEnabled());
                });

        add(div, updateStyle, updateEnableButton);
    }

}
