/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.vitelogout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * View displayed after successful logout.
 * <p>
 * Used to verify that the browser correctly navigated to this page after
 * session invalidation, instead of being redirected by Vite's page reload.
 */
@Route("com.vaadin.flow.uitest.ui.vitelogout.SessionEndedView")
public class SessionEndedView extends Div {

    public SessionEndedView() {
        Span marker = new Span("Session Ended Successfully");
        marker.setId("session-ended-marker");
        add(marker);
    }
}
