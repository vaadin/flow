/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.VaadinContext;

import javax.servlet.ServletContext;
import java.util.Set;

/**
 * Applies this initializer to the given {@link VaadinContext}.
 *
 * It is intended to be called either:
 * <ul>
 * <li>directly by non-servlet implementing HTTP frameworks or</li>
 * <li>indirectly on servlet container initialization (via
 * {@link ClassLoaderAwareServletContainerInitializer#onStartup(Set, ServletContext)})</li>
 * </ul>
 *
 * @since
 *
 * @see ClassLoaderAwareServletContainerInitializer
 * @see VaadinServletContextStartupInitializer
 */
@FunctionalInterface
public interface VaadinContextStartupInitializer {

    /**
     * Applies this initializer to the given context
     *
     * @param classSet
     *            the Set of application classes which this initializer needs to
     *            do its job
     *
     * @param context
     *            the {@link VaadinContext} to use with this initializer
     *
     * @throws VaadinInitializerException
     *             if an error has occurred
     */
    void initialize(Set<Class<?>> classSet, VaadinContext context)
            throws VaadinInitializerException;

}
