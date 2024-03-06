/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@Route(value = "com.vaadin.flow.uitest.ui.SessionCloseLogoutView")
@Push(transport = Transport.LONG_POLLING)
public class SessionCloseLogoutView extends Div {

    public SessionCloseLogoutView() {
        NativeButton btn = new NativeButton("Logout!");
        btn.addClickListener(evt -> getUI().ifPresent(ui -> {
            UI.getCurrent().getPage().setLocation(BaseHrefView.class.getName());
            ui.getSession().close();
        }));
        add(btn);
    }
}
