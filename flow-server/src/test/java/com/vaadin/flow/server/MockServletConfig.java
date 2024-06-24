/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import java.util.Enumeration;
import java.util.Properties;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockServletConfig implements ServletConfig {

    private ServletContext context = new MockServletContext();
    private static final Properties DEFAULT_PROPERTIES = new Properties();
    {
        DEFAULT_PROPERTIES.setProperty(
                InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());
    }

    private final Properties initParameters;

    public MockServletConfig() {
        this(DEFAULT_PROPERTIES);
    }

    public MockServletConfig(Properties initParameters) {
        this.initParameters = initParameters;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getServletName()
     */
    @Override
    public String getServletName() {
        return "Mock Servlet";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
     */
    @Override
    public String getInitParameter(String name) {
        return initParameters.getProperty(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getInitParameterNames()
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getInitParameterNames() {
        return initParameters.propertyNames();
    }

}
