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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.startup.AbstractConfigurationFactory;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Creates {@link DeploymentConfiguration} filled with all parameters specified
 * by the framework users.
 *
 * @since 1.2
 */
public class DeploymentConfigurationFactory extends AbstractConfigurationFactory
        implements Serializable {

    /**
     * Creates a {@link DeploymentConfiguration} instance that is filled with
     * all parameters, specified for the current app.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param vaadinConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     */
    public DeploymentConfiguration createDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig) {
        return new DefaultDeploymentConfiguration(
                ApplicationConfiguration.get(vaadinConfig.getVaadinContext()),
                systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, vaadinConfig));
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that has all
     * parameters, specified for the current app without doing checks so
     * property states and only returns default.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param vaadinConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     */
    public DeploymentConfiguration createPropertyDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig) {
        return new PropertyDeploymentConfiguration(
                ApplicationConfiguration.get(vaadinConfig.getVaadinContext()),
                systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, vaadinConfig));
    }

    /**
     * Generate Property containing parameters for with all parameters contained
     * in current application.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param vaadinConfig
     *            the config to get the rest of the properties from
     * @return {@link Properties} instance
     */
    protected Properties createInitParameters(Class<?> systemPropertyBaseClass,
            VaadinConfig vaadinConfig) {
        Properties initParameters = new Properties();
        readUiFromEnclosingClass(systemPropertyBaseClass, initParameters);

        // Override with application config from web.xml
        for (final Enumeration<String> e = vaadinConfig
                .getConfigParameterNames(); e.hasMoreElements();) {
            final String name = e.nextElement();
            String value = vaadinConfig.getConfigParameter(name);
            if (value != null) {
                initParameters.setProperty(name, value);
            } else {
                LoggerFactory.getLogger(DeploymentConfigurationFactory.class)
                        .debug("Ignoring NULL init parameter {}", name);
            }
        }

        readBuildInfo(initParameters);
        return initParameters;
    }

    private void readBuildInfo(Properties initParameters) {
        String json = getTokenFileContent(initParameters::getProperty);
        // Read the json and set the appropriate system properties if not
        // already set.
        if (json != null) {
            JsonNode buildInfo = JacksonUtils.readTree(json);
            Map<String, String> properties = getConfigParametersUsingTokenData(
                    buildInfo);
            // only insert properties that haven't been defined
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (!initParameters.containsKey(entry.getKey())) {
                    initParameters.put(entry.getKey(), entry.getValue());
                }
            }
        }

    }

    private static void readUiFromEnclosingClass(
            Class<?> systemPropertyBaseClass, Properties initParameters) {
        Class<?> enclosingClass = systemPropertyBaseClass.getEnclosingClass();

        if (enclosingClass != null
                && UI.class.isAssignableFrom(enclosingClass)) {
            initParameters.put(InitParameters.UI_PARAMETER,
                    enclosingClass.getName());
        }
    }

}
