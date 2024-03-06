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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.RemoveAddVisibilityView")
public class RemoveAddVisibilityView extends Div {

    public RemoveAddVisibilityView() {
        Span hidden = new Span("Initially hidden");
        hidden.setVisible(false);

        NativeButton toggle = new NativeButton("Make Element visible",
                event -> {
                    remove(hidden);
                    add(hidden);
                    hidden.setVisible(true);
                });
        toggle.setId("make-visible");

        add(toggle, hidden);
    }
}
