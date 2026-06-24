/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.vitelogout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

/**
 * View for testing Vite logout redirect behavior.
 * <p>
 * Contains a logout button that sets the location to the session-ended route
 * and invalidates the session. The test verifies that Vite's page reload
 * doesn't cancel the server-initiated redirect when the session is invalidated.
 */
@Route("com.vaadin.flow.uitest.ui.vitelogout.LogoutTestView")
public class LogoutTestView extends Div {

    public LogoutTestView() {
        Span marker = new Span("Logout Test View");
        marker.setId("logout-test-marker");

        NativeButton logoutButton = new NativeButton("Logout", e -> {
            UI.getCurrent().getPage().setLocation(
                    "/view/com.vaadin.flow.uitest.ui.vitelogout.SessionEndedView");
            VaadinSession.getCurrent().getSession().invalidate();
        });
        logoutButton.setId("logout-button");

        add(marker, logoutButton);
    }
}
