/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import javax.servlet.Servlet;

import java.util.Enumeration;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FallbackChunk;

/**
 * Configuration on the application level.
 * <p>
 * Configuration is based on {@link VaadinContext} which provides application
 * level data in contrast to {@link DeploymentConfiguration} which provides a
 * {@link Servlet} level configuration.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public interface ApplicationConfiguration extends AbstractConfiguration {

    /**
     * Gets a configuration instance for the given {code context}.
     * 
     * @param context
     *            the context to get the configuration for
     * @return the application level configuration for the given {@code context}
     */
    static ApplicationConfiguration get(VaadinContext context) {
        return context.getAttribute(ApplicationConfiguration.class, () -> {
            Lookup lookup = context.getAttribute(Lookup.class);
            ApplicationConfigurationFactory factory = lookup
                    .lookup(ApplicationConfigurationFactory.class);
            return factory.create(context);
        });
    }

    /**
     * Returns the names of the configuration properties as an
     * <code>Enumeration</code>, or an empty <code>Enumeration</code> if there
     * are o initialization parameters.
     *
     * @return configuration properties as a <code>Enumeration</code>
     */
    Enumeration<String> getPropertyNames();

    /**
     * The context which the configuration is based on.
     * 
     * @return the vaadin context
     */
    VaadinContext getContext();

    /**
     * Gets a fallback chunk for the application or {@code null} if it's not
     * available.
     * 
     * @return the application fallback chunk, may be {@code null}.
     */
    FallbackChunk getFallbackChunk();

}
