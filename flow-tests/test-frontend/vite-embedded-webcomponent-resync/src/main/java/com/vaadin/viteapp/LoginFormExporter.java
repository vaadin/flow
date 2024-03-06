/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class LoginFormExporter extends WebComponentExporter<LoginForm> {
    public LoginFormExporter() {
        super("login-form");
        addProperty("userlbl", "").onChange(LoginForm::setUserNameLabel);
        addProperty("pwdlbl", "").onChange(LoginForm::setPasswordLabel);
    }

    @Override
    protected void configureInstance(WebComponent<LoginForm> webComponent,
            LoginForm form) {
    }
}
