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
package com.vaadin.flow.server.startup;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouterUtil;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.ui.BodySize;
import com.vaadin.ui.Component;
import com.vaadin.ui.Inline;
import com.vaadin.ui.UI;
import com.vaadin.ui.Viewport;

/**
 * Common validation methods for route registry initializer.
 *
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractRouteRegistryInitializer {

    /**
     * Validate the potential route classes stream and return them as a set.
     *
     * @param routeClasses
     *            potential route classes
     * @return a resulting set of the route component classes
     */
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Component>> validateRouteClasses(
            Stream<Class<?>> routeClasses) {
        return routeClasses.peek(this::checkForConflictingAnnotations)
                .filter(this::isApplicableClass)
                .map(clazz -> (Class<? extends Component>) clazz)
                .collect(Collectors.toSet());
    }

    private boolean isApplicableClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Route.class)
                && Component.class.isAssignableFrom(clazz);
    }

    private void checkForConflictingAnnotations(Class<?> route) {
        if (route.isAnnotationPresent(RouteAlias.class)
                && !route.isAnnotationPresent(Route.class)) {
            throw new InvalidRouteLayoutConfigurationException(String.format(
                    "'%s'" + " declares '@%s' but doesn't declare '@%s'. "
                            + "The '%s' may not be used without '%s'",
                    route.getCanonicalName(), RouteAlias.class.getSimpleName(),
                    Route.class.getSimpleName(),
                    RouteAlias.class.getSimpleName(),
                    Route.class.getSimpleName()));

        }

        if (route.isAnnotationPresent(ParentLayout.class)) {
            throw new InvalidRouteLayoutConfigurationException(route
                    .getCanonicalName()
                    + " contains both @Route and @ParentLayout annotation. Only use @Route with Route.layout.");
        }
        if (route.isAnnotationPresent(PageTitle.class)
                && HasDynamicTitle.class.isAssignableFrom(route)) {
            throw new DuplicateNavigationTitleException(String.format(
                    "'%s' has a PageTitle annotation, but also implements HasDynamicTitle.",
                    route.getName()));
        }

        /* Validate annotation usage */
        Stream.of(Viewport.class, BodySize.class, Inline.class)
                .forEach(annotation -> {
                    validateRouteAnnotation(route, annotation);

                    for (RouteAlias alias : route
                            .getAnnotationsByType(RouteAlias.class)) {
                        validateRouteAliasAnnotation(route, alias, annotation);
                    }
                });

        /* Validate PageConfigurator usage */
        validateRouteImplementation(route, PageConfigurator.class);

        for (RouteAlias alias : route.getAnnotationsByType(RouteAlias.class)) {
            validateRouteAliasImplementation(route, alias,
                    PageConfigurator.class);
        }
    }

    /* Route validator methods for bootstrap implementations */
    private void validateRouteImplementation(Class<?> route,
            Class<?> implementation) {
        if (!UI.class.equals(route.getAnnotation(Route.class).layout())) {
            if (implementation.isAssignableFrom(route)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s needs to be the top parent layout '%s' not '%s'",
                                implementation.getSimpleName(),
                                RouterUtil.getTopParentLayout(route).getName(),
                                route.getName()));
            }

            List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                    .getParentLayouts(route);
            Class<? extends RouterLayout> topParentLayout = RouterUtil
                    .getTopParentLayout(route);

            validateParentImplementation(parentLayouts, topParentLayout,
                    implementation);
        }
    }

    private void validateRouteAliasImplementation(Class<?> route,
            RouteAlias alias, Class<?> implementation) {
        if (!UI.class.equals(alias.layout())) {
            if (PageConfigurator.class.isAssignableFrom(route)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s needs to be the top parent layout '%s' not '%s'",
                                implementation.getSimpleName(),
                                RouterUtil.getTopParentLayout(route,
                                        alias.value()).getName(),
                                route.getName()));
            }

            List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                    .getParentLayouts(route, alias.value());
            Class<? extends RouterLayout> topParentLayout = RouterUtil
                    .getTopParentLayout(route, alias.value());

            validateParentImplementation(parentLayouts, topParentLayout,
                    implementation);
        }
    }

    private void validateParentImplementation(
            List<Class<? extends RouterLayout>> parentLayouts,
            Class<? extends RouterLayout> topParentLayout,
            Class<?> implementation) {
        Supplier<Stream<Class<? extends RouterLayout>>> streamSupplier = () -> parentLayouts
                .stream().filter(implementation::isAssignableFrom);
        if (streamSupplier.get().count() > 1) {
            throw new InvalidRouteLayoutConfigurationException("Only one "
                    + implementation.getSimpleName()
                    + " implementation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                    + streamSupplier.get().map(Class::getName)
                            .collect(Collectors.joining(", ")));
        }

        streamSupplier.get().findFirst().ifPresent(layout -> {
            if (!layout.equals(topParentLayout)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s implementation should be the top most route layout '%s'. Offending class: '%s'",
                                implementation.getSimpleName(),
                                topParentLayout.getName(), layout.getName()));
            }
        });
    }

    /* Route validator methods for bootstrap annotations */
    private void validateRouteAnnotation(Class<?> route,
            Class<? extends Annotation> annotation) {
        if (!UI.class.equals(route.getAnnotation(Route.class).layout())) {
            if (route.isAnnotationPresent(annotation)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s annotation needs to be on the top parent layout '%s' not on '%s'",
                                annotation.getSimpleName(),
                                RouterUtil.getTopParentLayout(route).getName(),
                                route.getName()));
            }

            List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                    .getParentLayouts(route);
            Class<? extends RouterLayout> topParentLayout = RouterUtil
                    .getTopParentLayout(route);

            validateParentAnnotation(parentLayouts, topParentLayout,
                    annotation);
        }
    }

    private void validateRouteAliasAnnotation(Class<?> route, RouteAlias alias,
            Class<? extends Annotation> annotation) {
        if (!UI.class.equals(alias.layout())) {
            if (route.isAnnotationPresent(annotation)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s annotation needs to be on the top parent layout '%s' not on '%s'",
                                annotation.getSimpleName(),
                                RouterUtil.getTopParentLayout(route,
                                        alias.value()).getName(),
                                route.getName()));
            }

            List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                    .getParentLayouts(route, alias.value());
            Class<? extends RouterLayout> topParentLayout = RouterUtil
                    .getTopParentLayout(route, alias.value());

            validateParentAnnotation(parentLayouts, topParentLayout,
                    annotation);
        }
    }

    private void validateParentAnnotation(
            List<Class<? extends RouterLayout>> parentLayouts,
            Class<? extends RouterLayout> topParentLayout,
            Class<? extends Annotation> annotation) {
        Supplier<Stream<Class<? extends RouterLayout>>> streamSupplier = () -> parentLayouts
                .stream()
                .filter(layout -> layout.isAnnotationPresent(annotation));
        if (streamSupplier.get().count() > 1) {
            throw new InvalidRouteLayoutConfigurationException("Only one "
                    + annotation.getSimpleName()
                    + " annotation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                    + streamSupplier.get().map(Class::getName)
                            .collect(Collectors.joining(", ")));
        }

        streamSupplier.get().findFirst().ifPresent(layout -> {
            if (!layout.equals(topParentLayout)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s annotation should be on the top most route layout '%s'. Offending class: '%s'",
                                annotation.getSimpleName(),
                                topParentLayout.getName(), layout.getName()));
            }
        });
    }

}
