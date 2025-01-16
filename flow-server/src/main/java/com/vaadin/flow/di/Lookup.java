/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Provides a way to discover services used by Flow (SPI).
 * <p>
 * A lookup instance may be created based on a service, see
 * {@link #of(Object, Class...)}. Several lookup instances may be combined via
 * {@link #compose(Lookup, Lookup)} method which allows to make a lookup
 * instance based on a number of services. The resulting lookup instance may be
 * used in internal Flow code to transfer data in the unified way which allows
 * to change the available data types during the code evolution without changing
 * the internal API (like arguments in methods and constructors).
 * <p>
 * There is the "global" application {@link Lookup} instance and the
 * {@link VaadinContext}. It has one to one mapping and is available even before
 * a {@link DeploymentConfiguration} (and {@link VaadinServlet}) is created. So
 * this is kind of a singleton for a Web Application. As a consequence it
 * provides and may return only web app singleton services. Dependency injection
 * frameworks can provide an implementation for the application {@code Lookup}
 * which manages instances according to the conventions of that framework.
 * <p>
 * The application {@code Lookup} is similar to the {@link Instantiator} class
 * but a {@link Lookup} instance is available even before a
 * {@link VaadinService} instance is created (and as a consequence there is no
 * yet an {@link Instantiator} instance).
 * <p>
 * This is the code which one may use to get the application {@link Lookup}
 * instance:
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

    /**
     * Creates a lookup which contains (only) the provided {@code service} as
     * instance of given {@code serviceTypes}.
     * <p>
     * This method may be used to create a temporary lookup which then can be
     * used to extend an existing lookup via {@link #compose(Lookup, Lookup)}.
     *
     * @param <T>
     *            the service type
     * @param service
     *            the service object
     * @param serviceTypes
     *            the supertypes of the service which may be used to access the
     *            service
     * @return a lookup initialized with the given {@code service}
     */
    @SafeVarargs
    static <T> Lookup of(T service, Class<? super T>... serviceTypes) {
        Objects.requireNonNull(service);
        Set<Class<? super T>> services = Stream.of(serviceTypes).peek(type -> {
            if (!type.isInstance(service)) {
                throw new IllegalArgumentException(
                        "Service type" + service.getClass().getName()
                                + " is not a subtype of " + type.getName());
            }
        }).collect(Collectors.toSet());
        return new Lookup() {

            @Override
            public <U> Collection<U> lookupAll(Class<U> serviceClass) {
                U service = lookup(serviceClass);
                return service == null ? Collections.emptyList()
                        : Collections.singleton(service);
            }

            @Override
            public <U> U lookup(Class<U> serviceClass) {
                if (services.contains(serviceClass)) {
                    return serviceClass.cast(service);
                }
                return null;
            }
        };
    }

    /**
     * Make a composite lookup which contains the services from both
     * {@code lookup1} and {@code lookup2}.
     * <p>
     * {@link #lookup(Class)} method will return the service from the first
     * lookup if it's not null and fallbacks to the {@code lookup2} otherwise.
     * So the first lookup takes precedence. The method
     * {@link #lookupAll(Class)} simply combines all the services from both
     * lookups.
     * <p>
     * The resulting lookup is intended to be a "temporary" (short living)
     * lookup to extend an existing lookup with some additional data which is
     * required only in some isolated object.
     *
     * @param lookup1
     *            the first lookup to compose
     * @param lookup2
     *            the second lookup to compose
     * @return the composite lookup
     */
    static Lookup compose(Lookup lookup1, Lookup lookup2) {
        return new Lookup() {

            @Override
            public <T> Collection<T> lookupAll(Class<T> serviceClass) {
                return Stream
                        .concat(lookup1.lookupAll(serviceClass).stream(),
                                lookup2.lookupAll(serviceClass).stream())
                        .collect(Collectors.toList());
            }

            @Override
            public <T> T lookup(Class<T> serviceClass) {
                T service = lookup1.lookup(serviceClass);
                if (service == null) {
                    return lookup2.lookup(serviceClass);
                }
                return service;
            }
        };
    }
}
