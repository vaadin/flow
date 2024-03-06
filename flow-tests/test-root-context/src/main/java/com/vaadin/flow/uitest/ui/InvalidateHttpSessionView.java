/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

@Route("com.vaadin.flow.uitest.ui.InvalidateHttpSessionView")
public class InvalidateHttpSessionView extends Div {

    private static class SessionId {

        private String id;

        private SessionId(String id) {
            this.id = id;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        VaadinService service = attachEvent.getSession().getService();
        String id = attachEvent.getSession().getSession().getId();

        Div div = new Div();
        div.setText(id);
        div.setId("current-session-id");

        add(div);

        SessionId closedSessionId = attachEvent.getSession().getService()
                .getContext().getAttribute(SessionId.class);
        if (closedSessionId != null) {
            div = new Div();
            div.setText(closedSessionId.id);
            div.setId("invalidated-session-id");
            add(div);
        }

        service.addSessionDestroyListener(event -> {
            SessionId sessionId = new SessionId(id);
            service.getContext().setAttribute(SessionId.class, sessionId);
        });
        NativeButton button = new NativeButton("Invalidate HTTP session",
                event -> attachEvent.getSession().getSession().invalidate());
        add(button);
        button.setId("invalidate-session");
    }
}
