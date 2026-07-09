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
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;

@PreserveOnRefresh(partialMatch = true)
public class RootLayout extends Div implements RouterLayout {

    public static final String ROOT_EVENT_LOG_ID = "root-event-log";
    public static final String ROOT_RESET_ID = "root-reset-log";

    private static int eventCounter = 0;

    private final Div log = new Div();

    public RootLayout() {
        log.setText(++eventCounter + ": " + getClass().getSimpleName()
                + ": constructor");
        log.setId(ROOT_EVENT_LOG_ID);
        NativeButton reset = new NativeButton("Reset count",
                e -> eventCounter = 0);
        reset.setId(ROOT_RESET_ID);
        add(log, reset);
    }
}
