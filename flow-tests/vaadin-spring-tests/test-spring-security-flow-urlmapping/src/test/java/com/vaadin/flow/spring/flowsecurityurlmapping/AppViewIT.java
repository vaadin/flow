package com.vaadin.flow.spring.flowsecurityurlmapping;

public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {
    @Override
    protected String getUrlMappingBasePath() {
        return "/urlmapping";
    }
}
