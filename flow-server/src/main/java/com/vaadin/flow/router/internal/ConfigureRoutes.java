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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * Configuration class for editing routes. After editing the class should always
 * be set as a {@link ConfiguredRoutes} read only value object.
 * <p>
 * {@link ConfigureRoutes} is always mutable where as {@link ConfiguredRoutes} is always
 * immutable.
 */
public class ConfigureRoutes extends ConfiguredRoutes implements Serializable {

    protected final Map<String, RouteTarget> routes;
    protected final Map<Class<? extends Component>, String> targetRoutes;
    protected final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfigureRoutes() {
        routes = new HashMap<>();
        targetRoutes = new HashMap<>();
        exceptionTargets = new HashMap<>();
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *         original configuration to get data from
     */
    public ConfigureRoutes(ConfiguredRoutes original) {
        Map<String, RouteTarget> routes = new HashMap<>();
        Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.routes
                .entrySet()) {
            routes.put(route.getKey(), route.getValue().copy(true));
        }
        targetRoutes.putAll(original.targetRoutes);
        exceptionTargets.putAll(original.exceptionTargets);

        this.routes = routes;
        this.targetRoutes = targetRoutes;
        this.exceptionTargets = exceptionTargets;
    }

    @Override
    protected Map<String, RouteTarget> getRoutesMap() {
        return routes;
    }

    @Override
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return targetRoutes;
    }

    @Override
    public Map<Class<? extends Exception>, Class<? extends Component>> getExceptionHandlers() {
        return exceptionTargets;
    }

    /*-----------------------------------*/
    /* Mutation functions                */
    /*-----------------------------------*/

    /**
     * Clear all maps from this configuration.
     */
    public void clear() {
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
        if (hasRoute(path)) {
            routes.get(path).addRoute(navigationTarget);
        } else {
            routes.computeIfAbsent(path,
                    key -> new RouteTarget(navigationTarget, true));
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
        exceptionTargets.put(exception, errorTarget);
    }

    /**
     * Remove the targetRoute completely from the configuration.
     *
     * @param targetRoute
     *         target registered route to remove
     */
    public void removeRoute(Class<? extends Component> targetRoute) {
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
}
