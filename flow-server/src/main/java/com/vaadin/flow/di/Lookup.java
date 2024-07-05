/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.di;

import java.util.Collection;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Provides a way to discover services used by Flow (SPI). Dependency injection
 * frameworks can provide an implementation that manages instances according to
 * the conventions of that framework.
 * <p>
 * This is similar to the {@link Instantiator} class but a {@link Lookup}
 * instance is available even before a {@link VaadinService} instance is created
 * (and as a consequence there is no yet an {@link Instantiator} instance).
 * <p>
 * The {@link Lookup} instance and the {@link VaadinContext} has one to one
 * mapping and is available even before a {@link DeploymentConfiguration} ( and
 * {@link VaadinServlet}) is created. So this is kind of a singleton for a Web
 * Application. As a consequence it provides and may return only web app
 * singleton services.
 * <p>
 * This is the code which one may use to get the {@link Lookup} instance:
 *
 * <pre>
 * <code>
 *     VaadinContext context = ...;
 *     Lookup lookup = context.getAttribute(Lookup.class);
 * </code>
 * </pre>
 * <p>
 * This SPI is mostly for internal framework usage since {@link Instantiator}
 * provides all required services for the application developer.
 *
 *
 * @see Instantiator
 * @author Vaadin Ltd
 * @since
 */
public interface Lookup {

    /**
     * Lookup for a service of the given type.
     * <p>
     * The {@code serviceClass} is usually an interface (though it doesn't have
     * to be) and the returned value is some implementation of this interface.
     *
     * @param <T>
     *            a service type
     * @param serviceClass
     *            a service SPI class
     *
     * @see Lookup#lookupAll(Class)
     * @return a service which implements the {@code serviceClass}, may be
     *         {@code null} if no services are registered for this SPI
     */
    <T> T lookup(Class<T> serviceClass);

    /**
     * Lookup for all services by the provided {@code serviceClass}.
     * <p>
     * The {@code serviceClass} is usually an interface class (though it doesn't
     * have to be) and the returned value is all implementations of this
     * interface.
     *
     * @param <T>
     *            a service type
     * @param serviceClass
     *            a service SPI class
     * @return all services which implement the {@code serviceClass}, if no
     *         services found an empty list is returned (so {@code null} is not
     *         returned)
     */
    <T> Collection<T> lookupAll(Class<T> serviceClass);
}
