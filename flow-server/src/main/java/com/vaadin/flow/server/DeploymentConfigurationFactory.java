/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;

/**
 * A class that creates {@link DeploymentConfiguration} filled with all
 * parameters specified by the framework users.
 */
public final class DeploymentConfigurationFactory implements Serializable {

    private DeploymentConfigurationFactory() {
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that is filled with
     * all parameters, specified for the current app.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param servletConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for the parameters
     *             fails
     */
    public static DeploymentConfiguration createDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, ServletConfig servletConfig)
            throws ServletException {
        return new DefaultDeploymentConfiguration(systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, servletConfig));
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that has
     * all parameters, specified for the current app without doing checks so
     * property states and only returns default.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param servletConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for the parameters
     *             fails
     */
    public static DeploymentConfiguration createPropertyDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, ServletConfig servletConfig)
            throws ServletException {
        return new PropertyDeploymentConfiguration(systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, servletConfig));
    }

    /**
     * Generate Property containing parameters for with all parameters contained in current application.
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param servletConfig
     *            the config to get the rest of the properties from
     * @return {@link Properties} instance
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for the parameters
     *             fails
     */
    protected static Properties createInitParameters(
            Class<?> systemPropertyBaseClass, ServletConfig servletConfig)
            throws ServletException {
        Properties initParameters = new Properties();
        readUiFromEnclosingClass(systemPropertyBaseClass, initParameters);
        readConfigurationAnnotation(systemPropertyBaseClass, initParameters);

        // Read default parameters from server.xml
        final ServletContext context = servletConfig.getServletContext();
        for (final Enumeration<String> e = context.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, context.getInitParameter(name));
        }

        // Override with application config from web.xml
        for (final Enumeration<String> e = servletConfig
                .getInitParameterNames(); e.hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name,
                    servletConfig.getInitParameter(name));
        }
        return initParameters;
    }

    private static void readUiFromEnclosingClass(
            Class<?> systemPropertyBaseClass, Properties initParameters) {
        Class<?> enclosingClass = systemPropertyBaseClass.getEnclosingClass();

        if (enclosingClass != null
                && UI.class.isAssignableFrom(enclosingClass)) {
            initParameters.put(VaadinSession.UI_PARAMETER,
                    enclosingClass.getName());
        }
    }

    private static void readConfigurationAnnotation(
            Class<?> systemPropertyBaseClass, Properties initParameters)
            throws ServletException {
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
            assert name
                    != null : "All methods declared in VaadinServletConfiguration should have a @InitParameterName annotation";

            try {
                Object value = method.invoke(configuration);

                String stringValue;
                if (value instanceof Class<?>) {
                    stringValue = ((Class<?>) value).getName();
                } else {
                    stringValue = value.toString();
                }

                initParameters.setProperty(name.value(), stringValue);
            } catch (Exception e) {
                // This should never happen
                throw new ServletException(
                        "Could not read @VaadinServletConfiguration value "
                                + method.getName(), e);
            }
        }

    }
}
