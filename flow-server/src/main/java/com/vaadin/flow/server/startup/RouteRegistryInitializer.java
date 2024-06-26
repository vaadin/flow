/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.util.Set;

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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@HandlesTypes({ Route.class, RouteAlias.class })
public class RouteRegistryInitializer extends AbstractRouteRegistryInitializer
        implements ClassLoaderAwareServletContainerInitializer {

    @Override
    public void process(Set<Class<?>> classSet, ServletContext servletContext)
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
