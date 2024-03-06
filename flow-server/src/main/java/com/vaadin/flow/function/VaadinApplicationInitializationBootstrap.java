/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.function;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

/**
 * Represents Vaadin web application initialization bootstrap.
 * <p>
 * This is internal mechanism for bootstrapping Vaadin web application
 * initialization. It's executed before servlet initialization once the
 * {@code Lookup} instance is created. The internal implementation setups the
 * {@link Lookup} instance in the {@link VaadinContext} so that it becomes
 * available via {@link VaadinContext#getAttribute(Class)} and bootstraps all
 * initializers (basically {@link ServletContainerInitializer} impls) that
 * depends on {@link Lookup} presence.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface VaadinApplicationInitializationBootstrap {

    /**
     * Bootstraps Vaadin application initialization.
     *
     * @param lookup
     *            a lookup instance required for initialization
     * @throws ServletException
     *             if lookup initialization failed
     */
    void bootstrap(Lookup lookup) throws ServletException;
}
