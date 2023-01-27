/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.server.VaadinServletContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
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
