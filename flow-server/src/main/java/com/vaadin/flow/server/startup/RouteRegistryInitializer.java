/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import jakarta.servlet.annotation.HandlesTypes;

import java.util.Set;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.OneTimeInitializerPredicate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.VaadinContext;

/**
 * Servlet initializer for collecting all available {@link Route}s on startup.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@HandlesTypes({ Route.class, RouteAlias.class })
public class RouteRegistryInitializer extends AbstractRouteRegistryInitializer
        implements VaadinServletContextStartupInitializer {

    private static class PreviouslyStoredRoutesRegistry
            extends ApplicationRouteRegistry {

        private PreviouslyStoredRoutesRegistry(VaadinContext context) {
            super(context);
        }

    }

    @Override
    public void initialize(Set<Class<?>> classSet, VaadinContext context)
            throws VaadinInitializerException {
        try {
            Set<Class<?>> routesSet = AbstractAnnotationValidator
                    .removeHandleTypesSelfReferences(classSet, this);

            ApplicationRouteRegistry routeRegistry = ApplicationRouteRegistry
                    .getInstance(context);

            Set<Class<? extends Component>> routes = validateRouteClasses(
                    context, routesSet.stream());

            routeRegistry.update(() -> {
                if (removePreviousRoutes(context, routeRegistry)) {
                    configureStaticRoutesRegistry(context, routes);
                }

                configureRoutes(routes, routeRegistry);
            });
            routeRegistry.setPwaConfigurationClass(validatePwaClass(context,
                    routes.stream().map(clazz -> (Class<?>) clazz)));
        } catch (InvalidRouteConfigurationException irce) {
            throw new VaadinInitializerException(
                    "Exception while registering Routes on servlet startup",
                    irce);
        }
    }

    private void configureStaticRoutesRegistry(VaadinContext context,
            Set<Class<? extends Component>> routes) {
        PreviouslyStoredRoutesRegistry registry = new PreviouslyStoredRoutesRegistry(
                context);

        configureRoutes(routes, registry);
        context.setAttribute(registry);
    }

    private boolean removePreviousRoutes(VaadinContext context,
            ApplicationRouteRegistry registry) {
        Lookup lookup = context.getAttribute(Lookup.class);

        OneTimeInitializerPredicate oneTimeInitializer = lookup
                .lookup(OneTimeInitializerPredicate.class);

        if (oneTimeInitializer != null && oneTimeInitializer.runOnce()) {
            return false;
        }
        PreviouslyStoredRoutesRegistry prevoiusRegistry = context
                .getAttribute(PreviouslyStoredRoutesRegistry.class);
        if (prevoiusRegistry != null) {
            prevoiusRegistry.getRegisteredRoutes().forEach(routeData -> {
                registry.removeRoute(routeData.getTemplate());
                routeData.getRouteAliases().forEach(
                        alias -> registry.removeRoute(alias.getTemplate()));
            });
        }
        return true;
    }

    private void configureRoutes(Set<Class<? extends Component>> routes,
            ApplicationRouteRegistry routeRegistry) {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(routeRegistry);
        routeConfiguration
                .update(() -> setAnnotatedRoutes(routeConfiguration, routes));
    }

    private void setAnnotatedRoutes(RouteConfiguration routeConfiguration,
            Set<Class<? extends Component>> routes) {
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
