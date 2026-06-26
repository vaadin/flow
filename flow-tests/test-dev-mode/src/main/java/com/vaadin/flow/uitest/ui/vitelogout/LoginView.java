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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * Login view for testing Vite logout redirect behavior.
 * <p>
 * Contains a native HTML form that posts to the login route, which is
 * intercepted by {@link MockAuthenticationFilter}.
 */
@Route("com.vaadin.flow.uitest.ui.vitelogout.LoginView")
public class LoginView extends Div {

    public LoginView() {
        Element form = new Element("form");
        form.setAttribute("action",
                "/view/com.vaadin.flow.uitest.ui.vitelogout.LoginView");
        form.setAttribute("method", "POST");

        NativeButton submit = new NativeButton("Login");
        submit.setId("login-button");
        submit.getElement().setAttribute("type", "submit");

        getElement().appendChild(form);
        form.appendChild(submit.getElement());
    }
}
