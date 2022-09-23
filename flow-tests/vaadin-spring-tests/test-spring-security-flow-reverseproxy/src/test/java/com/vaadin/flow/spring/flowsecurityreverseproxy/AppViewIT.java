package com.vaadin.flow.spring.flowsecurityreverseproxy;

public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {

    private int publicProxyPort = 1234;
    private String publicProxyPath = "/public/path";

    @Override
    protected String getRootURL() {
        return super.getRootURL().replace("8888", publicProxyPort + "")
                + publicProxyPath;
    }
}
