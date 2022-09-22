package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.Route;

@Route("my/login/page")
public class LoginView extends LoginOverlay {

    public LoginView() {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("Spring Security - Flow Test Application");
        i18n.getHeader().setDescription(
                "Login using john/john (user) or emma/emma (admin)");
        i18n.setAdditionalInformation(null);
        setI18n(i18n);
        setForgotPasswordButtonVisible(false);
        setAction("my/login/page");
        setOpened(true);
    }
}
