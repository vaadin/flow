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
package com.vaadin.flow.server;

import java.io.Serializable;

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;

/**
 * Defines a base contract for configuration (e.g. on an application level,
 * servlet level,...).
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public interface AbstractConfiguration extends Serializable {
    /**
     * Returns whether Vaadin is in production mode.
     *
     * @return true if in production mode, false otherwise.
     */
    boolean isProductionMode();

    /**
     * Get if the dev server should be enabled. True by default
     *
     * @return true if dev server should be used
     */
    default boolean enableDevServer() {
        return getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER, true);
    }

    /**
     * Returns whether Vaadin is running in useDeprecatedV14Bootstrapping.
     *
     * @return true if in useDeprecatedV14Bootstrapping, false otherwise.
     */
    default boolean useV14Bootstrap() {
        return getBooleanProperty(SERVLET_PARAMETER_USE_V14_BOOTSTRAP, false);
    }

    /**
     * Gets a configured property as a string.
     *
     * @param name
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     */
    String getStringProperty(String name, String defaultValue);

    /**
     * Gets a configured property as a boolean.
     *
     *
     * @param name
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     *
     */
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Returns whether pnpm is enabled or not.
     *
     * @return {@code true} if enabled, {@code false} if not
     */
    default boolean isPnpmEnabled() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                Boolean.valueOf(Constants.ENABLE_PNPM_DEFAULT_STRING));
    }

}
