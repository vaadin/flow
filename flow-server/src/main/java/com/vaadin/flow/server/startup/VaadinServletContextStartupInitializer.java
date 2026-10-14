/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.VaadinServletContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows a library/runtime to be notified of a web application's startup phase
 * and perform any required programmatic registration of servlets, filters, and
 * listeners in response to it.
 *
 * @since
 *
 * @see ClassLoaderAwareServletContainerInitializer
 */
@FunctionalInterface
public interface VaadinServletContextStartupInitializer
        extends ClassLoaderAwareServletContainerInitializer,
        VaadinContextStartupInitializer {

    @Override
    default void process(Set<Class<?>> classSet, ServletContext context)
            throws ServletException {
        try {
            initialize(classSet, new VaadinServletContext(context));
        } catch (VaadinInitializerException e) {
            if (e.getCause() instanceof InvalidRouteConfigurationException || e
                    .getCause() instanceof InvalidRouteLayoutConfigurationException) {
                getLogger().error("Route configuration error found:");
                getLogger().error(e.getCause().getMessage());
            }
            throw new ServletException(e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(VaadinServletContextStartupInitializer.class);
    }
}
