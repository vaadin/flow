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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * Route configuration class that is used as a value object and can be mutable
 * or immutable.
 */
public class RouteConfiguration implements Serializable {

    private final boolean mutable;

    private final Map<String, RouteTarget> routes;
    private final Map<Class<? extends Component>, String> targetRoutes;
    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets;

    /**
     * Create an immutable RouteConfiguration.
     */
    public RouteConfiguration() {
        mutable = false;
        routes = Collections.emptyMap();
        targetRoutes = Collections.emptyMap();
        exceptionTargets = Collections.emptyMap();
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
        Map<String, RouteTarget> routes = new HashMap<>();
        Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.routes
                .entrySet()) {
            routes.put(route.getKey(), route.getValue().copy(mutable));
        }
        targetRoutes.putAll(original.targetRoutes);
        exceptionTargets.putAll(original.exceptionTargets);

        if (!mutable) {
            this.routes = routes.isEmpty() ?
                    Collections.emptyMap() :
                    Collections.unmodifiableMap(routes);
            this.targetRoutes = targetRoutes.isEmpty() ?
                    Collections.emptyMap() :
                    Collections.unmodifiableMap(targetRoutes);
            this.exceptionTargets = exceptionTargets.isEmpty() ?
                    Collections.emptyMap() :
                    Collections.unmodifiableMap(exceptionTargets);
        } else {
            this.routes = routes;
            this.targetRoutes = targetRoutes;
            this.exceptionTargets = exceptionTargets;
        }

        this.mutable = mutable;
    }

    private void throwIfImmutable() {
        if (!mutable) {
            throw new IllegalStateException(
                    "Tried to mutate immutable configuration.");
        }
    }

    public boolean isMutable() {
        return mutable;
    }

    /*-----------------------------------*/
    /* Mutation functions                */
    /* !All should throw is not mutable! */
    /*-----------------------------------*/

    /**
     * Clear all maps from this configuration.
     */
    public void clear() {
        throwIfImmutable();
        routes.clear();
        targetRoutes.clear();
    }

    /**
     * Set a new {@link RouteTarget} for the given path.
     * <p>
     * Note! this will override any previous value.
     *
     * @param path
     *         path for which to set route target for
     * @param navigationTarget
     *         navigation target to add
     */
    public void setRoute(String path,
            Class<? extends Component> navigationTarget) {
        throwIfImmutable();
        if (hasRoute(path)) {
            routes.get(path).addRoute(navigationTarget);
        } else {
            routes.computeIfAbsent(path,
                    key -> new RouteTarget(navigationTarget, isMutable()));
        }
    }

    /**
     * Put a new target route for Class-to-path mapping.
     * <p>
     * This is a reverse mapping to RouteTarget, which also handles any HasUrl
     * parameters, for the main route of this navigation target.
     *
     * @param navigationTarget
     *         navigation target to map
     * @param path
     *         path for given navigation target
     */
    public void setTargetRoute(Class<? extends Component> navigationTarget,
            String path) {
        throwIfImmutable();
        targetRoutes.put(navigationTarget, path);
    }

    /**
     * Set a error route to the configuration.
     * <p>
     * Any exception handler set for a existing error will override the old
     * exception handler.
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

    /**
     * Remove the targetRoute completely from the configuration.
     *
     * @param targetRoute
     *         target registered route to remove
     */
    public void removeRoute(Class<? extends Component> targetRoute) {
        throwIfImmutable();

        if (!hasRouteTarget(targetRoute)) {
            return;
        }

        // Remove target route from class-to-string map
        targetRoutes.remove(targetRoute);

        List<String> emptyRoutes = new ArrayList<>();
        // Remove all instances of the route class for any path
        // that it may be registered to
        routes.forEach((route, routeTarget) -> {
            routeTarget.remove(targetRoute);

            if (routeTarget.isEmpty()) {
                emptyRoutes.add(route);
            }
        });
        emptyRoutes.forEach(routes::remove);
    }

    /**
     * Remove route for given path. This will remove all targets registered for
     * given path.
     * <p>
     * In case there exists another path mapping for any of the removed route
     * targets the main class-to-string mapping will be updated to the first
     * found.
     *
     * @param path
     *         path from which to remove routes from
     */
    public void removeRoute(String path) {
        throwIfImmutable();

        if (!hasRoute(path)) {
            return;
        }

        RouteTarget removedRoute = routes.remove(path);
        for (Class<? extends Component> targetRoute : removedRoute
                .getRoutes()) {
            updateMainRouteTarget(targetRoute);
        }
    }

    /**
     * Remove specific navigation target for given route. The path will still
     * exist if there is another target with different parameters registered to
     * it. If no targets remain the path will be removed completely.
     * <p>
     * In case there exists another path mapping for the removed route
     * target the main class-to-string mapping will be updated to the first
     * found.
     *
     * @param path
     *         path to remove target from
     * @param targetRoute
     *         target route to remove from path
     */
    public void removeRoute(String path,
            Class<? extends Component> targetRoute) {
        throwIfImmutable();

        if (!hasRoute(path) || !routes.get(path).containsTarget(targetRoute)) {
            return;
        }

        RouteTarget routeTarget = routes.get(path);
        routeTarget.remove(targetRoute);

        if (routeTarget.isEmpty()) {
            routes.remove(path);
        }

        if (targetRoutes.containsKey(targetRoute) && targetRoutes
                .get(targetRoute).equals(path)) {
            updateMainRouteTarget(targetRoute);
        }
    }

    /**
     * Update the main route target for the navigationTarget if another route
     * for the class is found.
     *
     * @param navigationTarget
     *         navigation target to update the main route for
     */
    private void updateMainRouteTarget(
            Class<? extends Component> navigationTarget) {
        targetRoutes.remove(navigationTarget);

        // Update Class-to-string map with a new mapping if removed route exists for another path
        for (Map.Entry<String, RouteTarget> entry : routes.entrySet()) {
            if (entry.getValue().containsTarget(navigationTarget)) {
                targetRoutes.put(navigationTarget, entry.getKey());
                return;
            }
        }
    }

    /*---------------------------------*/
    /* Getters and other read methods. */
    /*---------------------------------*/

    /**
     * Collect all routes for which given routeTarget is registered.
     * This is mainly for handling route aliases as reading from the class
     * annotations doesn't specifically return the actual registartions as they
     * can change during runtime.
     *
     * @param routeTarget
     *         route target to collect registered paths for
     * @return list of routes this routeTarget is registered for
     */
    protected List<String> getRoutePaths(
            Class<? extends Component> routeTarget) {
        return routes.entrySet().stream()
                .filter(entry -> entry.getValue().containsTarget(routeTarget))
                .map(Map.Entry::getKey).collect(Collectors.toList());
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
     * Check it the given route target has been registered to the configuration.
     *
     * @param targetRoute
     *         target to check registration status for
     * @return true if target is found in configuration
     */
    public boolean hasRouteTarget(Class<? extends Component> targetRoute) {
        return targetRoutes.containsKey(targetRoute);
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
     * Get all registered paths that have been registered.
     *
     * @return Set containing all the registered paths
     */
    public Set<String> getRoutes() {
        return Collections.unmodifiableSet(routes.keySet());
    }

    /**
     * Get all registered target routes for this configuration.
     *
     * @return component-to-path map of all target routes
     */
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return targetRoutes;
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

    /**
     * Get all registered exception handlers as a exception-to-handler map.
     *
     * @return all registered exception handlers
     */
    public Map<Class<? extends Exception>, Class<? extends Component>> getExceptionHandlers() {
        return exceptionTargets;
    }

    /**
     * Return the parent layout chain for given navigation target on the target
     * path.
     *
     * @param path
     *         path to get parent layout chain for
     * @param navigationTarget
     *         navigation target on path to get parent layout chain for
     * @return list of parent layout chain
     */
    public List<Class<? extends RouterLayout>> getParentLayouts(String path,
            Class<? extends Component> navigationTarget) {
        return routes.get(path).getParentLayouts(navigationTarget);
    }

    /**
     * Get the RouteTarget stored for the given path.
     *
     * @param path
     *         path to get route target for
     * @return route target for path, <code>null</code> if nothing registered
     */
    protected RouteTarget getRouteTarget(String path) {
        return routes.get(path);
    }

}
