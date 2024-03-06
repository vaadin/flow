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
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.RedirectToPushView")
public class RedirectToPushView extends Div {

    public RedirectToPushView() {
        NativeButton button = new NativeButton("Redirect", event -> {
            getUI().get().navigate(PushSettingsView.class);
        });
        add(button);
    }

}
