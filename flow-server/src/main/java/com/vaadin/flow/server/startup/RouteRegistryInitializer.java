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
package com.vaadin.flow.server.startup;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Servlet initializer for collecting all available {@link Route}s on startup.
 *
 * @since 1.0
 */
@HandlesTypes({ Route.class, RouteAlias.class })
public class RouteRegistryInitializer extends AbstractRouteRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        VaadinServletContext context = new VaadinServletContext(servletContext);
        try {
            if (classSet == null) {
                ApplicationRouteRegistry routeRegistry = ApplicationRouteRegistry
                        .getInstance(context);
                routeRegistry.clean();
                return;
            }

            Set<Class<? extends Component>> routes = validateRouteClasses(
                    classSet.stream());

            ApplicationRouteRegistry routeRegistry = ApplicationRouteRegistry
                    .getInstance(context);

            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forRegistry(routeRegistry);
            routeConfiguration.update(
                    () -> setAnnotatedRoutes(routeConfiguration, routes));
            routeRegistry.setPwaConfigurationClass(validatePwaClass(
                    routes.stream().map(clazz -> (Class<?>) clazz)));
        } catch (InvalidRouteConfigurationException irce) {
            throw new ServletException(
                    "Exception while registering Routes on servlet startup",
                    irce);
        }
    }

    private void setAnnotatedRoutes(RouteConfiguration routeConfiguration,
            Set<Class<? extends Component>> routes) {
        routeConfiguration.getHandledRegistry().clean();
        for (Class<? extends Component> navigationTarget : routes) {
            try {
                routeConfiguration.setAnnotatedRoute(navigationTarget);
            } catch (AmbiguousRouteConfigurationException exception) {
                if (!handleAmbiguousRoute(routeConfiguration,
                        exception.getConfiguredNavigationTarget(),
                        navigationTarget)) {
                    throw exception;
                }
            }
        }
    }

    private boolean handleAmbiguousRoute(RouteConfiguration routeConfiguration,
            Class<? extends Component> configuredNavigationTarget,
            Class<? extends Component> navigationTarget) {
        if (GenericTypeReflector.isSuperType(navigationTarget,
                configuredNavigationTarget)) {
            return true;
        } else if (GenericTypeReflector.isSuperType(configuredNavigationTarget,
                navigationTarget)) {
            routeConfiguration.removeRoute(configuredNavigationTarget);
            routeConfiguration.setAnnotatedRoute(navigationTarget);
            return true;
        }
        return false;
    }

}
