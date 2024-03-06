/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.VaadinServletContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Set;

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
            throw new ServletException(e);
        }
    }

}
