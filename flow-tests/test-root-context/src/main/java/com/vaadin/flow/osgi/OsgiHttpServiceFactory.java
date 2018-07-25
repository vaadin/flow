package com.vaadin.flow.osgi;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

public class OsgiHttpServiceFactory implements ServiceFactory<HttpService> {

    private final ServletContext servletContext;

    public OsgiHttpServiceFactory(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public HttpService getService(Bundle bundle,
            ServiceRegistration<HttpService> registration) {
        return new OsgiHttpService(bundle, servletContext);
    }

    @Override
    public void ungetService(Bundle bundle,
            ServiceRegistration<HttpService> registration,
            HttpService service) {

    }

}
