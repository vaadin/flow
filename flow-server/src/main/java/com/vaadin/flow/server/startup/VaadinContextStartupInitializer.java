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
package com.vaadin.flow.server.startup;

import jakarta.servlet.ServletContext;

import java.util.Set;

import com.vaadin.flow.server.VaadinContext;

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
