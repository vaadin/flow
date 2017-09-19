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
package com.vaadin.server.startup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.ParentLayout;
import com.vaadin.annotations.Route;
import com.vaadin.annotations.RoutePrefix;
import com.vaadin.flow.router.Location;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class RouteRegistry {

    private final Map<String, Class<? extends Component>> routes = new HashMap<>();

    boolean initialized;

    private static final RouteRegistry INSTANCE = new RouteRegistry();

    private RouteRegistry() {
        initialized = false;
    }

    /**
     * Get the singleton instance of RouteRegistry.
     * 
     * @return the singleton instance of the registry
     */
    public static RouteRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a set of components as navigation targets.
     * <p>
     * <strong>Note:</strong> Navigation targets can only be set once, i.e. when
     * {@link #isInitialized()} is {@code false}.
     * 
     * @param navigationTargets
     *            set of navigation target components
     * @throws InvalidRouteConfigurationException
     *             if routing has been configured incorrectly
     */
    public void setNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        if (isInitialized()) {
            throw new InvalidRouteConfigurationException(
                    "Routes have already been initialized");
        }
        validateNavigationTargets(navigationTargets);
        doRegisterNavigationTargets(navigationTargets);
        initialized = true;
    }

    /**
     * Gets the optional navigation target class for a given Location. Returns
     * an empty optional if no navigation target corresponds to the given
     * Location.
     * 
     * @see Location
     * 
     * @param pathString
     *            the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given
     *         location
     */
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        return Optional.ofNullable(routes.get(pathString));
    }

    /**
     * Returns whether this registry has been initialized with navigation
     * targets.
     * 
     * @return whether this registry has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    private void validateNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        Map<String, Class<?>> navigationTargetMap = new HashMap<>();
        for (Class<?> navigationTarget : navigationTargets) {
            if (!navigationTarget.isAnnotationPresent(Route.class)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "No Route annotation is present for the given "
                                + "navigation target component '%s'.",
                        navigationTarget.getName()));
            }
            String route = getNavigationRoute(navigationTarget);
            if (navigationTargetMap.containsKey(route)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "Navigation targets must have unique routes, "
                                + "found navigation targets '%s' and '%s' with the same route.",
                        navigationTargetMap.get(route).getName(),
                        navigationTarget.getName()));
            }
            navigationTargetMap.put(route, navigationTarget);
        }
    }

    /**
     * Collect the whole route for the navigation target.
     * <p>
     * The whole route is composed of the Route annotation and any
     * ParentLayout:@RoutePrefix that may be in the navigation chain.
     * 
     * @param navigationTarget
     *            navigation target to get chain route for
     * @return full navigation route
     */
    private String getNavigationRoute(Class<?> navigationTarget) {
        Route annotation = navigationTarget.getAnnotation(Route.class);
        if (annotation.absolute()) {
            return annotation.value();
        }

        StringBuilder fullRoute = new StringBuilder();
        List<String> parentRoutePrefixes = getParentRoutePrefixes(
                navigationTarget);
        Collections.reverse(parentRoutePrefixes);

        parentRoutePrefixes
                .forEach(prefix -> fullRoute.append(prefix).append("/"));

        fullRoute.append(annotation.value());

        return fullRoute.toString();
    }

    private List<String> getParentRoutePrefixes(Class<?> component) {
        List<String> list = new ArrayList<>();

        Optional<Route> router = AnnotationReader.getAnnotationFor(component,
                Route.class);
        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(component, ParentLayout.class);
        Optional<RoutePrefix> routePrefix = AnnotationReader
                .getAnnotationFor(component, RoutePrefix.class);

        routePrefix.ifPresent(prefix -> list.add(prefix.value()));

        // break chain on an absolute RoutePrefix or Route
        if ((routePrefix.isPresent() && routePrefix.get().absolute())
                || (router.isPresent() && router.get().absolute())) {
            return list;
        }

        if (router.isPresent() && !router.get().layout().equals(UI.class)) {
            list.addAll(getParentRoutePrefixes(router.get().layout()));
        } else if (parentLayout.isPresent()) {
            list.addAll(getParentRoutePrefixes(parentLayout.get().value()));
        }

        return list;
    }

    private void doRegisterNavigationTargets(
            Set<Class<? extends Component>> navigationTargets) {
        routes.clear();
        navigationTargets.forEach(navigationTarget -> {
            String route = getNavigationRoute(navigationTarget);
            Logger.getLogger(RouteRegistry.class.getName()).log(Level.FINE,
                    String.format(
                            "Registering route '%s' to navigation target '%s'.",
                            route, navigationTarget.getName()));
            routes.put(route, navigationTarget);
        });
    }
}
