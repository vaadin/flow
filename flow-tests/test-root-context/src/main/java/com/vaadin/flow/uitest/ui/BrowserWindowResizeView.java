/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.BrowserWindowResizeView", layout = ViewTestLayout.class)
public class BrowserWindowResizeView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div windowSize = new Div();

        windowSize.setId("size-info");

        getPage().addBrowserWindowResizeListener(event -> windowSize.setText(
                "%sx%s".formatted(event.getWidth(), event.getHeight())));

        add(windowSize);

        var modalBtn = new NativeButton("Open modal (should keep working");
        modalBtn.setId("modal");
        modalBtn.addClickListener(e -> {
            add(new Div("Now modal, but resize events should still flow in"));
            getUI().get().addModal(new Div());
        });
        add(modalBtn);
    }
}
