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
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.LogoutView")
public class LogoutView extends Div {

    public LogoutView() {
        NativeButton logoutButton = new NativeButton("logout", ev -> {
            UI.getCurrent().getPage().setLocation(BaseHrefView.class.getName());
            UI.getCurrent().getSession().close();
        });
        add(logoutButton);
    }

}
