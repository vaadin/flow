/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui.partial;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;

@ParentLayout(RootLayout.class)
public class MainLayout extends Div implements RouterLayout {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String RESET_ID = "reset-log";

    private static int eventCounter = 0;

    private final Div log = new Div();

    public MainLayout() {
        log.setText(++eventCounter + ": " + getClass().getSimpleName()
                + ": constructor");
        log.setId(EVENT_LOG_ID);
        NativeButton reset = new NativeButton("Reset count",
                e -> eventCounter = 0);
        reset.setId(RESET_ID);
        add(log, reset);
    }
}
