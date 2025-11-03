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
import jakarta.servlet.ServletException;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Allows a library/runtime to be notified of a web application's startup phase
 * and perform any required programmatic registration of servlets, filters, and
 * listeners in response to it.
 *
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
            if (e.getCause() instanceof InvalidRouteConfigurationException || e
                    .getCause() instanceof InvalidRouteLayoutConfigurationException) {
                getLogger().error("Route configuration error found:");
                getLogger().error(e.getCause().getMessage());
            }
            throw new ServletException(e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(VaadinServletContextStartupInitializer.class);
    }
}
