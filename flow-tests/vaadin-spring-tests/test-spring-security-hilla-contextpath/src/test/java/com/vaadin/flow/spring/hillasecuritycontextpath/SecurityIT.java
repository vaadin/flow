package com.vaadin.flow.spring.hillasecuritycontextpath;

public class SecurityIT
        extends com.vaadin.flow.spring.hillasecurity.SecurityIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context";
    }

}
