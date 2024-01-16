/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
