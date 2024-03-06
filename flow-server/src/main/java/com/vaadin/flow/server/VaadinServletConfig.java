/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import java.util.Enumeration;
import java.util.Objects;

/**
 * {@link VaadinConfig} implementation for Servlets.
 *
 * @since
 */
public class VaadinServletConfig implements VaadinConfig {

    private transient ServletConfig config;

    /**
     * Vaadin servlet configuration wrapper constructor.
     *
     * @param config
     *            servlet configuration object, not <code>null</code>
     */
    public VaadinServletConfig(ServletConfig config) {
        Objects.requireNonNull(config,
                "VaadinServletConfig requires the ServletConfig object");
        this.config = config;
    }

    /**
     * Ensures there is a valid instance of {@link ServletConfig}.
     */
    private void ensureServletConfig() {
        if (config == null
                && VaadinService.getCurrent() instanceof VaadinServletService) {
            config = ((VaadinServletService) VaadinService.getCurrent())
                    .getServlet().getServletConfig();
        } else if (config == null) {
            throw new IllegalStateException(
                    "The underlying ServletContext of VaadinServletContext is null and there is no VaadinServletService to obtain it from.");
        }
    }

    @Override
    public VaadinContext getVaadinContext() {
        ensureServletConfig();
        return new VaadinServletContext(config.getServletContext());
    }

    @Override
    public Enumeration<String> getConfigParameterNames() {
        ensureServletConfig();
        return config.getInitParameterNames();
    }

    @Override
    public String getConfigParameter(String name) {
        ensureServletConfig();
        return config.getInitParameter(name);
    }
}
