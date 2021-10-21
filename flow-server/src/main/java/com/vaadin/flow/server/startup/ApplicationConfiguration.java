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
import com.vaadin.flow.server.InitParameters;
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
            if (lookup == null) {
                throw new IllegalStateException("The application "
                        + Lookup.class.getSimpleName()
                        + " instance is not found in the " + VaadinContext.class
                        + " instance. It means that "
                        + "the container has not executed "
                        + Lookup.class.getSimpleName()
                        + " initialization code: so either the container is not Servlet 3.0 compatible"
                        + " or project configuration is broken.");
            }
            ApplicationConfigurationFactory factory = lookup
                    .lookup(ApplicationConfigurationFactory.class);
            if (factory == null) {
                return null;
            }
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

    /**
     * Checks if development mode session serialization is enabled or not.
     * <p>
     * Disabling session serialization means all its
     * {@link com.vaadin.flow.component.UI} instances won't be serialized. This
     * might be needed if one or more <code>UI</code>'s are not serializable
     * and, thus, the whole http session might be discarded, making an
     * authentication or other sensitive data stored in the session to get lost,
     * which is not acceptable in most of the cases.
     * <p>
     * By default session serialization is disabled in development mode.
     *
     * @return {@code true} if dev mode session serialization is enabled,
     *         {@code false} otherwise
     */
    boolean isDevModeSessionSerializationEnabled();

    /**
     * Determines if Flow should automatically register servlets. For more
     * information on the servlets registered, refer to
     * {@link com.vaadin.flow.server.startup.ServletDeployer} javadoc.
     *
     * User can explicitly disable automatic servlet registration by setting the
     * {@link InitParameters#DISABLE_AUTOMATIC_SERVLET_REGISTRATION} property to
     * {@code true}.
     *
     * @return {@code true} if Flow should not automatically register servlets
     * @see com.vaadin.flow.server.startup.ServletDeployer
     */
    default boolean disableAutomaticServletRegistration() {
        return getBooleanProperty(
                InitParameters.DISABLE_AUTOMATIC_SERVLET_REGISTRATION, false);
    }

}
