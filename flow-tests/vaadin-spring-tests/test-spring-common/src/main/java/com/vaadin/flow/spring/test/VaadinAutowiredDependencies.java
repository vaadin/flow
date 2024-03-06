/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("vaadin-autowired-deps")
public class VaadinAutowiredDependencies extends Div {

    @Autowired
    private UI ui;

    @Autowired
    private VaadinSession session;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Div uiId = new Div();
        uiId.setId("ui-injected");
        uiId.setText(String.valueOf(ui.getUIId()) + ui.hashCode());

        Div currentUi = new Div();
        currentUi.setText(String.valueOf(UI.getCurrent().getUIId())
                + UI.getCurrent().hashCode());
        currentUi.setId("ui-current");

        Div sessionDiv = new Div();
        sessionDiv.setText(ui.getCsrfToken() + session.hashCode());
        sessionDiv.setId("session-injected");

        Div sessionCurrent = new Div();
        sessionCurrent.setText(UI.getCurrent().getCsrfToken()
                + VaadinSession.getCurrent().hashCode());
        sessionCurrent.setId("session-current");

        add(uiId, currentUi, sessionDiv, sessionCurrent);

    }
}
