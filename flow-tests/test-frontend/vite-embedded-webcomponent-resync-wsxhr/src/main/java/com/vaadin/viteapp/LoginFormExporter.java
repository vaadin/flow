package com.vaadin.viteapp;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.WEBSOCKET_XHR)
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
