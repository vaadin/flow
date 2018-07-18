package com.vaadin.flow.osgi;

import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class OsgiHttpService implements HttpService {

    private final Bundle bundle;

    private final ServletContext servletContext;

    public OsgiHttpService(Bundle bundle, ServletContext servletContext) {
        this.bundle = bundle;
        this.servletContext = servletContext;
    }

    @Override
    public void registerServlet(String alias, Servlet servlet,
            Dictionary initparams, HttpContext context)
            throws ServletException, NamespaceException {

    }

    @Override
    public void registerResources(String alias, String path,
            HttpContext context) throws NamespaceException {
        if (context == null) {
            context = createDefaultHttpContext();
        }
        Dynamic servlet = servletContext.addServlet(
                "Resources servlet for path " + path,
                new StaticResourceServlet(path, context));
        servlet.addMapping(alias + "/*");
    }

    @Override
    public void unregister(String alias) {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new HttpContextImpl(bundle);
    }

}
