/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import java.util.Optional;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.ui.UI;

/**
 * An abstract base class for DeploymentConfiguration implementations. This
 * class provides default implementation for common config properties.
 *
 * @since 7.4
 *
 * @author Vaadin Ltd
 */
public abstract class AbstractDeploymentConfiguration
        implements DeploymentConfiguration {

    @Override
    public String getUIClassName() {
        return getStringProperty(VaadinSession.UI_PARAMETER,
                UI.class.getName());
    }

    @Override
    public String getClassLoaderName() {
        return getStringProperty("ClassLoader", null);
    }

    @Override
    public String getRouterConfiguratorClassName() {
        return getStringProperty(
                Constants.SERVLET_PARAMETER_ROUTER_CONFIGURATOR, null);
    }

    /**
     * Gets Vaadin system property value.
     * If no property found, the default value is returned.
     *
     * @param propertyName the property name to search for
     * @param defaultValue the value to be returned if system property would be absent
     * @return corresponding property value if present, default value otherwise
     */
    public static String getVaadinSystemProperty(String propertyName, String defaultValue) {
        String systemProperty = Optional
                .ofNullable(System.getProperty("vaadin." + propertyName))
                .orElseGet(() -> System.getProperty(propertyName));
        return systemProperty == null ? defaultValue : systemProperty;
    }
}
