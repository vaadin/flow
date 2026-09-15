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
package com.vaadin.quarkus.graal;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.atmosphere.cpr.AtmosphereFramework;

/**
 * Defers the initialization of executors at runtime to prevent failures during
 * native build.
 */
public class AtmosphereDeferredInitializer {

    transient List<AtmosphereFramework> frameworks = new ArrayList<>();

    /**
     * Called by @Recorder at RUNTIME_INIT to complete deferred Atmosphere
     * initialization that cannot be performed at build time (e.g. starting
     * thread pools).
     *
     * @param servletContext
     *            the servlet context.
     */
    static void completeInitialization(ServletContext servletContext) {
        AtmosphereDeferredInitializer initializer = getOrCreateInitializer(
                servletContext);
        initializer.frameworks.forEach(DelayedInitBroadcaster::startExecutors);
    }

    /**
     * Takes a reference to an Atmosphere instance to defer the initialization
     * of Atmosphere at RUNTIME_INIT phase. The effective initialization is
     * performed by the AtmosphereDeferredInitializerRecorder @Recoder.
     *
     * @param config
     *            the servlet config.
     * @param framework
     *            the Atmosphere framework instance.
     */
    public static void register(ServletConfig config,
            AtmosphereFramework framework) {
        ServletContext context = config.getServletContext();
        AtmosphereDeferredInitializer initializer = getOrCreateInitializer(
                context);
        initializer.frameworks.add(framework);
    }

    private static AtmosphereDeferredInitializer getOrCreateInitializer(
            ServletContext context) {
        AtmosphereDeferredInitializer initializer = (AtmosphereDeferredInitializer) context
                .getAttribute(AtmosphereDeferredInitializer.class.getName());
        if (initializer == null) {
            initializer = new AtmosphereDeferredInitializer();
            context.setAttribute(AtmosphereDeferredInitializer.class.getName(),
                    initializer);
        }
        return initializer;
    }
}
