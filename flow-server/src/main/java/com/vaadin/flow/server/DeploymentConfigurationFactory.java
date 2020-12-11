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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.AbstractConfigurationFactory;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_TOKEN_FILE;

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

    private static final Logger logger = LoggerFactory
            .getLogger(DeploymentConfigurationFactory.class);

    /**
     * Creates a {@link DeploymentConfiguration} instance that is filled with
     * all parameters, specified for the current app.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param vaadinConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     * @throws VaadinConfigurationException
     *             thrown if property construction fails
     */
    public DeploymentConfiguration createDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig)
            throws VaadinConfigurationException {
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
     * @throws VaadinConfigurationException
     *             thrown if property construction fails
     */
    public DeploymentConfiguration createPropertyDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig)
            throws VaadinConfigurationException {
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
     * @throws VaadinConfigurationException
     *             thrown if property construction fails
     */
    protected Properties createInitParameters(Class<?> systemPropertyBaseClass,
            VaadinConfig vaadinConfig) throws VaadinConfigurationException {
        Properties initParameters = new Properties();
        readUiFromEnclosingClass(systemPropertyBaseClass, initParameters);
        readConfigurationAnnotation(systemPropertyBaseClass, initParameters);

        // Override with application config from web.xml
        for (final Enumeration<String> e = vaadinConfig
                .getConfigParameterNames(); e.hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name,
                    vaadinConfig.getConfigParameter(name));
        }

        readBuildInfo(systemPropertyBaseClass, initParameters,
                vaadinConfig.getVaadinContext());
        return initParameters;
    }

    private void readBuildInfo(Class<?> systemPropertyBaseClass,
            Properties initParameters, VaadinContext context) {
        String json = getTokenFileContents(initParameters);

        // Read the json and set the appropriate system properties if not
        // already set.
        if (json != null) {
            JsonObject buildInfo = JsonUtil.parse(json);
            Map<String, String> properties = getInitParametersUsingTokenData(
                    buildInfo);
            initParameters.putAll(properties);

            FallbackChunk fallbackChunk = FrontendUtils
                    .readFallbackChunk(buildInfo);
            if (fallbackChunk != null) {
                initParameters.put(FALLBACK_CHUNK, fallbackChunk);
            }
        }
    }

    private static String getTokenFileContents(Properties initParameters) {
        try {
            return getResourceFromFile(initParameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getResourceFromFile(Properties initParameters)
            throws IOException {
        String json = null;
        // token file location passed via init parameter property
        String tokenLocation = initParameters.getProperty(PARAM_TOKEN_FILE);
        if (tokenLocation != null) {
            File tokenFile = new File(tokenLocation);
            if (tokenFile != null && tokenFile.canRead()) {
                json = FileUtils.readFileToString(tokenFile,
                        StandardCharsets.UTF_8);
            }
        }
        return json;
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

    /**
     * Read the VaadinServletConfiguration annotation for initialization name
     * value pairs and add them to the initial properties object.
     *
     * @param systemPropertyBaseClass
     *            base class for constructing the configuration
     * @param initParameters
     *            current initParameters object
     * @throws VaadinConfigurationException
     *             exception thrown for failure in invoking method on
     *             configuration annotation
     */
    private static void readConfigurationAnnotation(
            Class<?> systemPropertyBaseClass, Properties initParameters)
            throws VaadinConfigurationException {
        Optional<VaadinServletConfiguration> optionalConfigAnnotation = AnnotationReader
                .getAnnotationFor(systemPropertyBaseClass,
                        VaadinServletConfiguration.class);

        if (!optionalConfigAnnotation.isPresent()) {
            return;
        }

        VaadinServletConfiguration configuration = optionalConfigAnnotation
                .get();
        Method[] methods = VaadinServletConfiguration.class
                .getDeclaredMethods();
        for (Method method : methods) {
            VaadinServletConfiguration.InitParameterName name = method
                    .getAnnotation(
                            VaadinServletConfiguration.InitParameterName.class);
            assert name != null : "All methods declared in VaadinServletConfiguration should have a @InitParameterName annotation";

            try {
                Object value = method.invoke(configuration);

                String stringValue;
                if (value instanceof Class<?>) {
                    stringValue = ((Class<?>) value).getName();
                } else {
                    stringValue = value.toString();
                }

                initParameters.setProperty(name.value(), stringValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                // This should never happen
                throw new VaadinConfigurationException(
                        "Could not read @VaadinServletConfiguration value "
                                + method.getName(),
                        e);
            }
        }
    }
}