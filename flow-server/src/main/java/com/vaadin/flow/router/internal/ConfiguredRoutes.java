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
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * Route configuration class that is used as a value object.
 * <p>
 * Note! This is always immutable and any changes should be made from {@link
 * ConfigureRoutes}.
 *
 * @since 1.3
 */
public class ConfiguredRoutes implements Serializable {

    private final Map<String, RouteTarget> routes;
    private final Map<Class<? extends Component>, String> targetRoutes;
    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfiguredRoutes() {
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
     */
    public ConfiguredRoutes(ConfigureRoutes original) {
        Map<String, RouteTarget> routeMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRouteMap = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            routeMap.put(route.getKey(), route.getValue().copy(false));
        }
        targetRouteMap.putAll(original.getTargetRoutes());
        exceptionTargetMap.putAll(original.getExceptionHandlers());

        this.routes = routeMap.isEmpty() ?
                Collections.emptyMap() :
                Collections.unmodifiableMap(routeMap);
        this.targetRoutes = targetRouteMap.isEmpty() ?
                Collections.emptyMap() :
                Collections.unmodifiableMap(targetRouteMap);
        this.exceptionTargets = exceptionTargetMap.isEmpty() ?
                Collections.emptyMap() :
                Collections.unmodifiableMap(exceptionTargetMap);
    }

    protected Map<String, RouteTarget> getRoutesMap() {
        return routes;
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
        return getRoutesMap().entrySet().stream()
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
        return getRoutesMap().containsKey(path);
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
        if (getRoutesMap().containsKey(pathString)) {
            return getRoutesMap().get(pathString).getTarget(segments) != null;
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
        return getTargetRoutes().containsKey(targetRoute);
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
        if (getRoutesMap().containsKey(pathString)) {
            RouteTarget routeTarget = getRoutesMap().get(pathString);
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
        return Collections.unmodifiableSet(getRoutesMap().keySet());
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
     * Get the route path String for the given navigation target class.
     *
     * @param navigationTarget
     *         navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getTargetRoute(Class<? extends Component> navigationTarget) {
        return getTargetRoutes().get(navigationTarget);
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
        return getExceptionHandlers().get(exceptionClass);
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
        return getRoutesMap().get(path).getParentLayouts(navigationTarget);
    }

    /**
     * Get the RouteTarget stored for the given path.
     *
     * @param path
     *         path to get route target for
     * @return route target for path, <code>null</code> if nothing registered
     */
    protected RouteTarget getRouteTarget(String path) {
        return getRoutesMap().get(path);
    }

}
