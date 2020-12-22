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
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.AbstractConfigurationFactory;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Creates {@link DeploymentConfiguration} filled with all parameters specified
 * by the framework users.
 *
 * @since 1.2
 */
public class DeploymentConfigurationFactory extends AbstractConfigurationFactory
        implements Serializable {

    public static final Object FALLBACK_CHUNK = new Serializable() {
    };

    public static final String ERROR_DEV_MODE_NO_FILES = "There are neither 'flow-build-info.json' nor 'webpack.config.js' file available in "
            + "the project/working directory. Ensure 'webpack.config.js' is present or trigger creation of "
            + "'flow-build-info.json' via running 'prepare-frontend' Maven goal.";

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
            initParameters.setProperty(name,
                    vaadinConfig.getConfigParameter(name));
        }

        readBuildInfo(initParameters, vaadinConfig.getVaadinContext());
        return initParameters;
    }

    private void readBuildInfo(Properties initParameters,
            VaadinContext context) {
        String json = getTokenFileContent(initParameters::getProperty);

        FallbackChunk fallbackChunk = null;

        // Read the json and set the appropriate system properties if not
        // already set.
        if (json != null) {
            JsonObject buildInfo = JsonUtil.parse(json);
            Map<String, String> properties = getConfigParametersUsingTokenData(
                    buildInfo);
            initParameters.putAll(properties);

            fallbackChunk = FrontendUtils.readFallbackChunk(buildInfo);
        }
        if (fallbackChunk == null) {
            fallbackChunk = ApplicationConfiguration.get(context)
                    .getFallbackChunk();
        }
        if (fallbackChunk != null) {
            initParameters.put(FALLBACK_CHUNK, fallbackChunk);
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
