package com.vaadin.flow.spring.flowsecuritycontextpath;

public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context";
    }
}
