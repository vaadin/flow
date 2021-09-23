/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.RouteRegistry;

/**
 * Utility class with methods for route handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.3
 */
public class RouteUtil {

    protected RouteUtil() {
    }

    /**
     * Get parent layouts for navigation target according to the {@link Route}
     * or {@link RouteAlias} annotation.
     *
     * @param component
     *            navigation target to get parents for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component, String path) {
        final List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);

        if (route.isPresent()
                && path.equals(getRoutePath(component, route.get()))
                && !route.get().layout().equals(UI.class)) {
            list.addAll(collectRouteParentLayouts(route.get().layout()));
        } else {
            Optional<RouteAliasObject> matchingRoute = getMatchingRouteAlias(
                    component, path, getAliasObjects(component));
            if (matchingRoute.isPresent()) {
                list.addAll(collectRouteParentLayouts(
                        matchingRoute.get().getLayout()));
            }
        }

        return list;
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *            navigation target component to get route path for
     * @param route
     *            route annotation to check
     * @return actual path for given route target
     */
    public static String getRoutePath(Class<?> component, Route route) {
        if (route.absolute()) {
            return resolve(component, route);
        }
        List<String> parentRoutePrefixes = getRoutePrefixes(component,
                route.layout(), resolve(component, route));
        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *            navigation target component to get route path for
     * @param alias
     *            route alias annotation to check
     * @return actual path for given route alias target
     */
    public static String getRouteAliasPath(Class<?> component,
            RouteAlias alias) {
        return getRouteAliasPath(component, new RouteAliasObject(alias));
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *            navigation target component to get route path for
     * @param alias
     *            route alias data
     * @return actual path for given route alias target
     */
    public static String getRouteAliasPath(Class<?> component,
            RouteAliasObject alias) {
        if (alias.isAbsolute()) {
            return alias.getAlias();
        }
        List<String> parentRoutePrefixes = getRoutePrefixes(component,
                alias.getLayout(), alias.getAlias());
        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    private static List<String> getRoutePrefixes(Class<?> component,
            final Class<? extends RouterLayout> layout, final String value) {
        List<String> parentRoutePrefixes = getParentRoutePrefixes(component,
                () -> layout);
        Collections.reverse(parentRoutePrefixes);
        if (value != null && !value.isEmpty()) {
            parentRoutePrefixes.add(value);
        }

        return parentRoutePrefixes;
    }

    private static List<String> getParentRoutePrefixes(Class<?> component,
            Supplier<Class<? extends RouterLayout>> routerLayoutSupplier) {
        List<String> list = new ArrayList<>();

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(component, ParentLayout.class);
        Optional<RoutePrefix> routePrefix = AnnotationReader
                .getAnnotationFor(component, RoutePrefix.class);

        routePrefix.ifPresent(prefix -> list.add(prefix.value()));

        // break chain on an absolute RoutePrefix or Route
        if (routePrefix.isPresent() && routePrefix.get().absolute()) {
            return list;
        }

        Class<? extends RouterLayout> routerLayout = routerLayoutSupplier.get();
        if (routerLayout != null && !routerLayout.equals(UI.class)) {
            list.addAll(getParentRoutePrefixes(routerLayout, () -> null));
        } else if (parentLayout.isPresent()) {
            list.addAll(getParentRoutePrefixes(parentLayout.get().value(),
                    () -> null));
        }

        return list;
    }

    static Optional<RouteAliasObject> getMatchingRouteAlias(Class<?> component,
            String path, Stream<RouteAliasObject> routeAliases) {
        return routeAliases.filter(
                alias -> path.equals(getRouteAliasPath(component, alias))
                        && !alias.getLayout().equals(UI.class))
                .findFirst();
    }

    static List<Class<? extends RouterLayout>> collectRouteParentLayouts(
            Class<? extends RouterLayout> layout) {
        List<Class<? extends RouterLayout>> layouts = new ArrayList<>();
        layouts.add(layout);

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(layout, ParentLayout.class);
        if (parentLayout.isPresent()) {
            layouts.addAll(
                    collectRouteParentLayouts(parentLayout.get().value()));
        }
        return layouts;
    }

    /**
     * Collect possible route parent layouts for a navigation target that is not
     * annotated with {@link Route} nor {@link RouteAlias}, but may still
     * contain {@link ParentLayout}. Mainly error navigation targets.
     *
     * @param navigationTarget
     *            route to check parent layouts for
     * @return list of parent layouts
     */
    public static List<Class<? extends RouterLayout>> getParentLayoutsForNonRouteTarget(
            Class<?> navigationTarget) {
        List<Class<? extends RouterLayout>> layouts = new ArrayList<>();

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(navigationTarget, ParentLayout.class);
        if (parentLayout.isPresent()) {
            layouts.addAll(
                    collectRouteParentLayouts(parentLayout.get().value()));
        }
        return layouts;
    }

    /**
     * Get the top most parent layout for navigation target according to the
     * {@link Route} or {@link RouteAlias} annotation. Also handles non route
     * targets with {@link ParentLayout}.
     *
     * @param component
     *            navigation target to get top most parent for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle or null for error views.
     * @return top parent layout for target or null if none found
     */
    public static Class<? extends RouterLayout> getTopParentLayout(
            final Class<?> component, final String path) {
        if (path == null) {
            Optional<ParentLayout> parentLayout = AnnotationReader
                    .getAnnotationFor(component, ParentLayout.class);
            if (parentLayout.isPresent()) {
                return recurseToTopLayout(parentLayout.get().value());
            }
            // No need to check for Route or RouteAlias as the path is null
            return null;
        }

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        if (route.isPresent()
                && path.equals(getRoutePath(component, route.get()))
                && !route.get().layout().equals(UI.class)) {
            return recurseToTopLayout(route.get().layout());
        } else {
            Optional<RouteAliasObject> matchingRoute = getMatchingRouteAlias(
                    component, path, getAliasObjects(component));
            if (matchingRoute.isPresent()) {
                return recurseToTopLayout(matchingRoute.get().getLayout());
            }
        }

        return null;
    }

    private static Class<? extends RouterLayout> recurseToTopLayout(
            Class<? extends RouterLayout> layout) {
        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(layout, ParentLayout.class);

        if (parentLayout.isPresent()) {
            return recurseToTopLayout(parentLayout.get().value());
        }
        return layout;
    }

    /**
     * Gets the effective route path value of the annotated class.
     *
     * @param component
     *            the component where the route points to
     * @param route
     *            the annotation
     * @return The value of the annotation or naming convention based value if
     *         no explicit value is given.
     * @deprecated Use {@link RoutePathProvider}
     *             {@link DefaultRoutePathProvider} instead
     */
    @Deprecated
    public static String resolve(Class<?> component, Route route) {
        if (route.value().length == 0) {
            String simpleName = component.getSimpleName();
            if ("MainView".equals(simpleName) || "Main".equals(simpleName)) {
                return "";
            }
            if (simpleName.endsWith("View")) {
                return simpleName
                        .substring(0, simpleName.length() - "View".length())
                        .toLowerCase();
            }
            return simpleName.toLowerCase();
        }
        return route.value()[0];
    }

    /**
     * Updates route registry as necessary when classes have been added /
     * modified / deleted.
     * 
     * @param registry
     *            route registry
     * @param addedClasses
     *            added classes
     * @param modifiedClasses
     *            modified classes
     * @param deletedClasses
     *            deleted classes
     */
    public static void updateRouteRegistry(RouteRegistry registry,
            Set<Class<?>> addedClasses, Set<Class<?>> modifiedClasses,
            Set<Class<?>> deletedClasses) {
        RouteConfiguration routeConf = RouteConfiguration.forRegistry(registry);

        Logger logger = LoggerFactory.getLogger(RouteUtil.class);

        registry.update(() -> {
            // remove deleted classes and classes that lost the annotation from
            // registry
            Stream.concat(deletedClasses.stream(),
                    modifiedClasses.stream().filter(
                            clazz -> !clazz.isAnnotationPresent(Route.class)))
                    .filter(Component.class::isAssignableFrom)
                    .forEach(clazz -> {
                        Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
                        logger.debug("Removing route to {}", componentClass);
                        routeConf.removeRoute(componentClass);
                    });
            // add new routes to registry
            Stream.concat(addedClasses.stream(), modifiedClasses.stream())
                    .distinct().filter(Component.class::isAssignableFrom)
                    .filter(clazz -> clazz.isAnnotationPresent(Route.class))
                    .forEach(clazz -> {
                        Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
                        logger.debug(
                                "Updating route {} to {}", componentClass
                                        .getAnnotation(Route.class).value(),
                                clazz);
                        routeConf.removeRoute(componentClass);
                        routeConf.setAnnotatedRoute(componentClass);
                    });
        });
    }

    /**
     * Gets a stream {@link RouteAliasObject} of for the
     * {@code navigationTarget}.
     * <p>
     * Uses {@link Route} and {@link RouteAlias} annotations of the
     * {@code navigationTarget} class and returns all alias data collected using
     * these annotations.
     * 
     * @param navigationTarget
     *            a navigation target class to collect alias data
     * @return a stream of route alias objects
     */
    public static Stream<RouteAliasObject> getAliasObjects(
            Class<?> navigationTarget) {
        Route routeAnnotation = navigationTarget.getAnnotation(Route.class);
        if (routeAnnotation == null) {
            return Stream
                    .of(navigationTarget.getAnnotationsByType(RouteAlias.class))
                    .map(RouteAliasObject::new);
        } else {
            Stream<RouteAliasObject> aliases = Stream
                    .of(routeAnnotation.value()).skip(1)
                    .map(path -> new RouteAliasObject(path,
                            routeAnnotation.layout(),
                            routeAnnotation.absolute()));
            return Stream.concat(aliases, Stream
                    .of(navigationTarget.getAnnotationsByType(RouteAlias.class))
                    .map(RouteAliasObject::new));
        }
    }
}
