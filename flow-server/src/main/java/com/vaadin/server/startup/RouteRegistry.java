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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.vaadin.annotations.Route;
import com.vaadin.flow.router.Location;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.ui.Component;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class RouteRegistry {

    private final Map<String, Class<? extends Component>> routes = new HashMap<>();

    private boolean initialized;

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
     * <strong>Note:</strong> Navigation targets can only be set once!
     * 
     * @param navigationTargets
     *            set of navigation target components
     */
    public void setNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        if (initialized) {
            throw new InvalidRouteConfigurationException(
                    "Routes have already been initialized");
        }
        validateNavigationTargets(navigationTargets);
        doRegisterRoutes(navigationTargets);
        initialized = true;
    }

    /**
     * Gets the optional navigation target class for a given Location. Returns
     * an empty optional if no navigation target corresponds to the given
     * Location.
     * 
     * @see Location
     * 
     * @param location
     *            the location to get the navigation target for, not
     *            {@code null}
     * @return optional of the navigation target corresponding to the given
     *         location
     */
    public Optional<Class<? extends Component>> getNavigationTarget(
            Location location) {
        Objects.requireNonNull(location, "Location must not be null.");
        return Optional.ofNullable(routes.get(location.getPath()));
    }

    private boolean validateNavigationTargets(
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
            String route = navigationTarget.getAnnotation(Route.class).value();
            if (navigationTargetMap.containsKey(route)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "Navigation targets must have unique routes, "
                                + "found navigation targets '%s' and '%s' with the same route.",
                        navigationTargetMap.get(route).getName(),
                        navigationTarget.getName()));
            }
            navigationTargetMap.put(route, navigationTarget);
        }
        return true;
    }

    private void doRegisterRoutes(Set<Class<? extends Component>> routes) {
        this.routes.clear();
        routes.forEach(route -> {
            this.routes.put(route.getAnnotation(Route.class).value(), route);
        });
    }
}
