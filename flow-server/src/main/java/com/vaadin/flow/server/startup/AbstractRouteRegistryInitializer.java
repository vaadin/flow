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
package com.vaadin.flow.server.startup;

import jakarta.servlet.annotation.HandlesTypes;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinContext;

/**
 * Common validation methods for route registry initializer.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractRouteRegistryInitializer implements Serializable {
    private Class<?> pwaClass = null;

    /**
     * Validate the potential route classes stream and return them as a set.
     *
     * @param context
     *            a Vaadin context
     * @param routeClasses
     *            potential route classes
     * @return a resulting set of the route component classes
     */
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Component>> validateRouteClasses(
            VaadinContext context, Stream<Class<?>> routeClasses) {

        return routeClasses
                .peek(clazz -> checkForConflictingAnnotations(context, clazz))
                .filter(this::isApplicableClass)
                .map(target -> (Class<? extends Component>) target)
                .collect(Collectors.toSet());
    }

    /**
     * Any navigation target applicable to be registered on startup should be a
     * {@link Component}, contain the {@link Route} annotation and have
     * {@link Route#registerAtStartup} be {@code true}.
     *
     * @param clazz
     *            class to check for filer
     * @return true if applicable class
     */
    private boolean isApplicableClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Route.class)
                && Component.class.isAssignableFrom(clazz)
                && clazz.getAnnotation(Route.class).registerAtStartup();
    }

    private void checkForConflictingAnnotations(VaadinContext context,
            Class<?> route) {
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

        RouteAlias[] aliases = route.getAnnotationsByType(RouteAlias.class);
        if (aliases.length > 0) {
            String routePath = RouteUtil.getRoutePath(context, route);
            Map<String, Long> stats = Arrays.stream(aliases)
                    .map(ann -> RouteUtil.getRouteAliasPath(route, ann))
                    .collect(Collectors.groupingBy(Function.identity(),
                            Collectors.counting()));
            if (stats.containsKey(routePath)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "'%s' declares '@%s' and '@%s' with the same path '%s'. "
                                + "Make sure paths are different by checking annotation values "
                                + "and prefixes defined by layouts",
                        route.getCanonicalName(), Route.class.getSimpleName(),
                        RouteAlias.class.getSimpleName(), routePath));
            }
            String repeatedAliases = stats.entrySet().stream()
                    .filter(e -> e.getValue() > 1).map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
            if (!repeatedAliases.isEmpty()) {
                throw new InvalidRouteConfigurationException(String.format(
                        "'%s' declares multiple '@%s' with same paths: %s."
                                + "Make sure paths are different by checking annotation values "
                                + "and prefixes defined by layouts.",
                        route.getCanonicalName(),
                        RouteAlias.class.getSimpleName(), repeatedAliases));
            }
        }

        if (route.isAnnotationPresent(PageTitle.class)
                && HasDynamicTitle.class.isAssignableFrom(route)) {
            throw new DuplicateNavigationTitleException(String.format(
                    "'%s' has a PageTitle annotation, but also implements HasDynamicTitle.",
                    route.getName()));
        }

        /* Validate annotation usage */
        getValidationAnnotations().forEach(type -> {
            Class<? extends Annotation> annotation = type
                    .asSubclass(Annotation.class);

            validateRouteAnnotation(context, route, annotation);

            for (RouteAlias alias : aliases) {
                validateRouteAliasAnnotation(context, route, alias, annotation);
            }
        });

        validateRouteParentLayout(route);
    }

    private Stream<Class<?>> getValidationAnnotations() {
        return Stream.concat(
                Stream.of(AnnotationValidator.class
                        .getAnnotation(HandlesTypes.class).value()),
                Stream.of(WebComponentExporterAwareValidator.class
                        .getAnnotation(HandlesTypes.class).value()));
    }

    /* Route validator methods for bootstrap implementations */
    private void validateRouteParentLayout(Class<?> route) {
        Route annotation = route.getAnnotation(Route.class);
        ParentLayout parentLayout = route.getAnnotation(ParentLayout.class);
        if (annotation == null || parentLayout == null) {
            return;
        }
        if (!RouterLayout.class.isAssignableFrom(route)) {
            throw new InvalidRouteLayoutConfigurationException(String.format(
                    "The class '%s' should either be a '%s' or only a navigation target using"
                            + " '%s.layout' to set the parent layout",
                    route.getSimpleName(), RouterLayout.class.getSimpleName(),
                    Route.class.getSimpleName()));
        }
    }

    /* Route validator methods for bootstrap annotations */
    private void validateRouteAnnotation(VaadinContext context, Class<?> route,
            Class<? extends Annotation> annotation) {
        Route routeAnnotation = route.getAnnotation(Route.class);
        if (routeAnnotation != null
                && !UI.class.equals(routeAnnotation.layout())) {
            if (route.isAnnotationPresent(annotation)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s annotation needs to be on the top parent layout '%s' not on '%s'",
                                annotation.getSimpleName(),
                                RouteUtil.getTopParentLayout(context, route,
                                        RouteUtil.resolve(context, route))
                                        .getName(),
                                route.getName()));
            }

            List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                    .getParentLayouts(context, route,
                            RouteUtil.resolve(context, route));
            Class<? extends RouterLayout> topParentLayout = RouteUtil
                    .getTopParentLayout(context, route,
                            RouteUtil.resolve(context, route));

            validateParentAnnotation(parentLayouts, topParentLayout,
                    annotation);
        }
    }

    private void validateRouteAliasAnnotation(VaadinContext context,
            Class<?> route, RouteAlias alias,
            Class<? extends Annotation> annotation) {
        if (!UI.class.equals(alias.layout())) {
            if (route.isAnnotationPresent(annotation)) {
                throw new InvalidRouteLayoutConfigurationException(String
                        .format("%s annotation needs to be on the top parent layout '%s' not on '%s'",
                                annotation.getSimpleName(),
                                RouteUtil.getTopParentLayout(context, route,
                                        alias.value()).getName(),
                                route.getName()));
            }

            List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                    .getParentLayouts(context, route, alias.value());
            Class<? extends RouterLayout> topParentLayout = RouteUtil
                    .getTopParentLayout(context, route, alias.value());

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

    /**
     * Validate PWA annotations of the potential route classes stream, search
     * for properly annotated PWA class and return it, or null if none existing.
     *
     * @param context
     *            a Vaadin context
     * @param routeClasses
     *            potential route classes
     * @return a PWA -annotated class, or null if none exist.
     */
    @SuppressWarnings("unchecked")
    protected Class<?> validatePwaClass(VaadinContext context,
            Stream<Class<?>> routeClasses) {
        pwaClass = null;
        routeClasses.forEach(route -> {
            // check and validate route pwa annotation
            validatePwa(route);

            Route routeAnnotation = route.getAnnotation(Route.class);

            if (!UI.class.equals(routeAnnotation.layout())) {
                Class<? extends RouterLayout> topParentLayout = RouteUtil
                        .getTopParentLayout(context, route,
                                RouteUtil.resolve(context, route));
                // check and validate top parent layout pwa annotation
                validatePwa(topParentLayout);
            }
        });
        return pwaClass;
    }

    private void validatePwa(Class<?> pwaClassCandidate) {
        if (pwaClassCandidate == null
                || !pwaClassCandidate.isAnnotationPresent(PWA.class)) {
            return;
        } else if (pwaClass == null || pwaClass == pwaClassCandidate) {
            pwaClass = pwaClassCandidate;
        } else {
            throw new InvalidRouteLayoutConfigurationException(String.format(
                    "Expected only one '%s' annotation that is placed on the main layout of the application. Got multiple annotations in '%s' and '%s'",
                    PWA.class.getSimpleName(), pwaClass.getSimpleName(),
                    pwaClassCandidate.getSimpleName()));
        }
    }

}
