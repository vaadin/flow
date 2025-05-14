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

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;

import java.util.Collection;
import java.util.Map;

import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

/**
 * SPI for customizing lookup in applications inside Servlet 5.0 containers.
 * <p>
 * There are two ways of customizing Lookup in various servlet containers:
 * <ul>
 * <li>Use {@link AbstractLookupInitializer} SPI via providing an implementation
 * for the framework which doesn't prevent
 * {@link LookupServletContainerInitializer} execution.
 * <li>Completely disable {@link LookupServletContainerInitializer} and
 * implement own way to set up {@link Lookup} and make it available via
 * {@link VaadinContext#getAttribute(Class)}.
 * </ul>
 *
 * The first case allows to customize {@link Lookup} creation and initialization
 * in case when it's not possible to prevent
 * {@link LookupServletContainerInitializer} execution (any container which
 * completely supports Servlet 5.0 specification). In this case it's possible to
 * implement {@link AbstractLookupInitializer} for the framework.
 * <p>
 * The second case is only possible when a servlet container doesn't run
 * {@link ServletContainerInitializer}s out of the box (e.g. OSGi or Spring boot
 * executed as a Jar) at all. Otherwise you may not disable an existing
 * {@link ServletContainerInitializer} and it will be executed anyway.
 * <p>
 * This is SPI for {@link Lookup} SPI. The difference is:
 * <ul>
 * <li>{@link Lookup} allows to override services per Web application (by the
 * application developer). For some service interfaces there can be several
 * implementations available in {@link Lookup}.
 * <li>{@link AbstractLookupInitializer} allows to override how the
 * {@link Lookup} works per framework. The default implementation available if
 * no framework is used. Only one service implementation (excluding the default
 * one) may be available in the web application classpath and it's provided by
 * the developers for the framework support (the main usecase here is Spring
 * add-on).
 * </ul>
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface AbstractLookupInitializer {

    /**
     * Creates a new {@link Lookup} instance, initializes it and passes it to
     * the provided {@code bootstrap}.
     * <p>
     * The method should creates a new initialized {@link Lookup} instance. In
     * some cases it's not possible to create the instance right away when the
     * method is called. To be able to support this usecase the method contract
     * doesn't require to return the {@link Lookup} instance. Instead the
     * created instance should be passed to the provided {@code bootstrap}
     * consumer once the instance is created and completely initialized. The
     * {@code bootstrap} will start the application initialization which
     * otherwise is postponed until a {@link Lookup} becomes available.
     * <p>
     * The implementation must use the provided {@code bootstrap} to pass the
     * {@link Lookup} instance otherwise the web application based on this
     * {@link LookupInitializer} will never be bootstrapped.
     * <p>
     * The provided {@code services} map contains service implementations found
     * in application classpath using {@code @HandlesTypes} annotation declared
     * for {@link LookupServletContainerInitializer}.
     *
     * @param context
     *            a Vaadin context to run initialization for
     * @param services
     *            the map of internal services with their implementations found
     *            in the application classpath
     * @param bootstrap
     *            the web application bootstrap
     * @throws ServletException
     *             if initialization failed
     */
    void initialize(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services,
            VaadinApplicationInitializationBootstrap bootstrap)
            throws ServletException;
}
