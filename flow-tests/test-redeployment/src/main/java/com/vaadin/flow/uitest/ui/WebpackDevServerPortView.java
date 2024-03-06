/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.base.devserver.WebpackHandler;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

@Route(value = "com.vaadin.flow.uitest.ui.WebpackDevServerPortView")
public class WebpackDevServerPortView extends Div {

    public static final String UUID_ID = "uuid";
    public static final String WEBPACK_PORT_ID = "webpackPortId";
    public static final String TRIGGER_RELOAD_ID = "triggerReload";

    private static final UUID uuid = UUID.randomUUID();

    public WebpackDevServerPortView() {
        // Add a unique number to identify reload
        Span unique = new Span(String.valueOf(uuid));
        unique.setId(UUID_ID);
        add(unique);

        WebpackHandler handler = (WebpackHandler) DevModeHandlerManager
                .getDevModeHandler(VaadinService.getCurrent()).orElse(null);
        Span portSpan = new Span(String.valueOf(handler.getPort()));
        portSpan.setId(WEBPACK_PORT_ID);
        add(portSpan);

        final NativeButton triggerButton = new NativeButton("Trigger reload",
                event -> Application.triggerReload());
        triggerButton.setId(TRIGGER_RELOAD_ID);
        add(triggerButton);
    }

}
