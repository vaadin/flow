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
 *
 */
package com.vaadin.server.startup;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.FilterRegistration;
import javax.servlet.Registration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebServlet;

import com.vaadin.server.AbstractDeploymentConfiguration;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServlet;

/**
 * Initializer that adds webjar filter that would be used to redirect certain
 * requests webjars' contents. Refer to {@link WebJarFilter} for more details.
 * <p>
 * The initializer searches for {@link VaadinServlet} classes in order to get a
 * servlet configuration. A filter is only registered when exactly one
 * {@link VaadinServlet} is found.
 *
 * @see WebJarFilter
 */
@HandlesTypes(VaadinServlet.class)
public class WebjarFilterInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        Optional<ServletRegistration> servletRegistration = getVaadinServletRegistration(
                classSet, servletContext);
        if (servletRegistration.isPresent()
                && shouldAddFilter(servletRegistration.get())) {
            addWebjarFilter(servletContext, servletRegistration.get());
        } else if (!servletRegistration.isPresent()) {
            Logger.getLogger(getClass().getName()).config(String.format(
                    "Could not find exactly one Vaadin Servlet, not registering webjar filters. Servlets: '%s'",
                    classSet));
        }
    }

    private Optional<ServletRegistration> getVaadinServletRegistration(
            Collection<Class<?>> classSet, ServletContext servletContext) {
        if (classSet == null || classSet.size() != 1) {
            return Optional.empty();
        }
        return Optional
                .ofNullable(classSet.iterator().next()
                        .getDeclaredAnnotation(WebServlet.class))
                .map(WebServlet::name)
                .map(servletContext::getServletRegistration);
    }

    private boolean shouldAddFilter(Registration servletRegistration) {
        return !getBooleanParameterValue(servletRegistration,
                Constants.SERVLET_PARAMETER_PRODUCTION_MODE)
                && !getBooleanParameterValue(servletRegistration,
                        Constants.DISABLE_WEBJARS);
    }

    private boolean getBooleanParameterValue(Registration servletRegistration,
            String parameterName) {
        String stringValue = Optional
                .ofNullable(servletRegistration.getInitParameter(parameterName))
                .orElseGet(() -> AbstractDeploymentConfiguration
                        .getVaadinSystemProperty(parameterName, null));
        return Boolean.parseBoolean(stringValue);
    }

    private void addWebjarFilter(ServletContext servletContext,
            Registration servletRegistration) {
        FilterRegistration.Dynamic configurator = servletContext
                .addFilter(WebJarFilter.class.getName(), WebJarFilter.class);
        configurator.addMappingForUrlPatterns(null, true, WebJarFilter.PATTERN);
        configurator.setInitParameters(servletRegistration.getInitParameters());
    }
}
