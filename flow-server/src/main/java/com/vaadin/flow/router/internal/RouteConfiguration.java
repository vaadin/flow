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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * Route configuration class that is used as a value object and can be mutable
 * or immutable.
 */
public class RouteConfiguration implements Serializable {

    private final boolean mutable;

    private final Map<String, RouteTarget> routes = new HashMap<>();
    private final Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();
    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets = new HashMap<>();

    /**
     * Create an immutable RouteConfiguration
     */
    public RouteConfiguration() {
        mutable = false;
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *         original configuration to get data from
     * @param mutable
     *         true for mutable configuration and false for immutable
     */
    public RouteConfiguration(RouteConfiguration original, boolean mutable) {
        for (Map.Entry<String, RouteTarget> route : original.routes
                .entrySet()) {
            routes.put(route.getKey(), route.getValue().copy());
        }
        targetRoutes.putAll(original.targetRoutes);
        exceptionTargets.putAll(original.exceptionTargets);

        this.mutable = mutable;
    }

    /**
     * Clear all maps from this configuration.
     */
    public void clear() {
        throwIfImmutable();
        routes.clear();
        targetRoutes.clear();
    }

    private void throwIfImmutable() {
        if (!mutable) {
            throw new IllegalStateException(
                    "Tried to mutate immutable configuration.");
        }
    }

    /**
     * See if configuration contains a registered route for given path.
     *
     * @param path
     *         path to check
     * @return true if configuration contains route
     */
    public boolean hasRoute(String path) {
        return routes.containsKey(path);
    }

    /**
     * Add a navigation target for the given path.
     * <p>
     * Note! Before using this it should be certain that {@link
     * #hasRoute(String)} returns true. If not then {@link
     * #setRouteTarget(String, RouteTarget)} should be used.
     *
     * @param path
     *         path to add route to
     * @param navigationTarget
     *         navigation target to add
     */
    public void addRouteTarget(String path,
            Class<? extends Component> navigationTarget) {
        throwIfImmutable();
        if (routes.get(path) == null) {
            throw new IllegalArgumentException(
                    "No RouteTarget initialized for path");
        }
        routes.get(path).addRoute(navigationTarget);
    }

    /**
     * Set a new {@link RouteTarget} for the given path.
     * <p></p>
     * Note! this will override any previous value. It's recommended to use
     * {@link #addRouteTarget(String, Class)} if {@link #hasRoute(String)}
     * returns true
     *
     * @param path
     *         path for which to set route target for
     * @param routeTarget
     *         route target to set
     */
    public void setRouteTarget(String path, RouteTarget routeTarget) {
        throwIfImmutable();
        routes.put(path, routeTarget);
    }

    /**
     * Put a new target route for Class-to-path mapping.
     *
     * @param navigationTarget
     *         navigation target to map
     * @param path
     *         path for given navigation target
     */
    public void putTargetRoute(Class<? extends Component> navigationTarget,
            String path) {
        throwIfImmutable();
        targetRoutes.put(navigationTarget, path);
    }

    /**
     * Set a error route to the configuration.
     *
     * @param exception
     *         exception handled by error route
     * @param errorTarget
     *         error navigation target
     */
    public void setErrorRoute(Class<? extends Exception> exception,
            Class<? extends Component> errorTarget) {
        throwIfImmutable();
        exceptionTargets.put(exception, errorTarget);
    }

    /*---------------------------------*/
    /* Getters and other read methods. */
    /*---------------------------------*/

    /**
     * Check if configuration holds a route for given path with possible path
     * segments.
     *
     * @param pathString
     *         path string to check
     * @param segments
     *         path segments for route
     * @return true if a route is found, else false
     */
    public boolean hasRoute(String pathString, List<String> segments) {
        if (routes.containsKey(pathString)) {
            return routes.get(pathString).getTarget(segments) != null;
        }
        return false;
    }

    /**
     * Get the route class matching the given path and path segments.
     *
     * @param pathString
     *         string to get the route for
     * @param segments
     *         possible path segments
     * @return {@link Optional} containing the navigationTarget class if found
     */
    public Optional<Class<? extends Component>> getRoute(String pathString,
            List<String> segments) {
        if (routes.containsKey(pathString)) {
            RouteTarget routeTarget = routes.get(pathString);
            return Optional.ofNullable(routeTarget.getTarget(segments));
        }
        return Optional.empty();
    }

    /**
     * Configuration is empty if no routes have been registered.
     *
     * @return if there are registered routes
     */
    public boolean isEmpty() {
        return routes.isEmpty();
    }

    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return Collections.unmodifiableMap(targetRoutes);
    }

    /**
     * Get if we have any exception targets stored.
     *
     * @return true if there are exception targets
     */
    public boolean hasExceptionTargets() {
        return !exceptionTargets.isEmpty();
    }

    /**
     * Get a exception handler by exception class.
     *
     * @param exceptionClass
     *         exception class to get exception handler for
     * @return exception handler if found
     */
    public Class<? extends Component> getExceptionHandlerByClass(
            Class<?> exceptionClass) {
        return exceptionTargets.get(exceptionClass);
    }

    /**
     * Get the route path String for the given navigation target class.
     *
     * @param navigationTarget
     *         navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getTargetRoute(Class<? extends Component> navigationTarget) {
        return targetRoutes.get(navigationTarget);
    }
}
