/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

/**
 * Internal collection of initializers which may not be executed immediately but
 * requires a {@link Lookup} instance which will be set in the
 * {@link VaadinContext} ({@link ServletContext}) only when
 * {@link LookupServletContainerInitializer} completed.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
class DeferredServletContextInitializers {

    /**
     * A callback which will be called to run
     * {@link ServletContainerInitializer} logic once a {@link ServletContext}
     * is initialized with {@link Lookup}.
     *
     * @author Vaadin Ltd
     * @since
     *
     */
    interface Initializer {
        /**
         * Runs {@link ServletContainerInitializer} logic with the provided
         * {@code context}.
         *
         * @param context
         *            a ServletContext for the initializer
         * @throws ServletException
         *             thrown if the initializer throws an exception
         */
        void init(ServletContext context) throws ServletException;
    }

    private final List<Initializer> initializers = new CopyOnWriteArrayList<>();

    /**
     * Adds deferred initializer.
     *
     * @param initializer
     *            an initializer
     */
    void addInitializer(Initializer initializer) {
        initializers.add(initializer);
    }

    /**
     * Runs all collected initializers.
     *
     * @param context
     *            a ServletContext for initializers
     * @throws ServletException
     *             thrown if some initializer throws an exception
     */
    void runInitializers(ServletContext context) throws ServletException {
        for (Initializer initializer : initializers) {
            initializer.init(context);
        }
    }
}
