/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DevToolsPluginView", layout = ViewTestLayout.class)
public class DevToolsPluginView extends AbstractDivView {

    @Override
    protected void onShow() {
        add(new Span(
                "This is a dummy view that can be updated from a dev tools plugin"));

        NativeButton refresh = new NativeButton("Refresh");
        refresh.setId("refresh");
        refresh.addClickListener(e -> {
            // Just causes the state to be synced without push
        });
        add(refresh);
    }

}
