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

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.InvalidRouteConfigurationException;

/**
 * Servlet initializer for collecting all available {@link Route}s on startup.
 */
@HandlesTypes({ Route.class, RouteAlias.class })
public class RouteRegistryInitializer extends AbstractRouteRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        try {
            if (classSet == null) {
                ApplicationRouteRegistry routeRegistry = ApplicationRouteRegistry
                        .getInstance(servletContext);
                routeRegistry.clean();
                return;
            }

            Set<Class<? extends Component>> routes = validateRouteClasses(
                    classSet.stream());

            ApplicationRouteRegistry routeRegistry = ApplicationRouteRegistry
                    .getInstance(servletContext);

            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forRegistry(routeRegistry);
            routeConfiguration.update(() -> {
                routeConfiguration.getHandledRegistry().clean();
                for (Class<? extends Component> navigationTarget : routes) {
                    routeConfiguration.setAnnotatedRoute(navigationTarget);
                }
            });
            routeRegistry.setPwaConfigurationClass(validatePwaClass(
                    routes.stream().map(clazz -> (Class<?>) clazz)));
        } catch (InvalidRouteConfigurationException irce) {
            throw new ServletException(
                    "Exception while registering Routes on servlet startup",
                    irce);
        }
    }

}
