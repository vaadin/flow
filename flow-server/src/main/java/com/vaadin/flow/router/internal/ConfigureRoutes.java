/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasUrlParameterUtil;

/**
 * Configuration class for editing routes. After editing the class should
 * always
 * be set as a {@link ConfiguredRoutes} read only value object.
 * <p>
 * {@link ConfigureRoutes} is always mutable where as {@link ConfiguredRoutes}
 * is always
 * immutable.
 *
 * @since 1.3
 */
public class ConfigureRoutes extends ConfiguredRoutes implements Serializable {

    // Stores targets accessed by urls with parameters.
    private final RouteModel routeModel;

    private final Map<String, RouteTarget> routeMap;
    private final Map<Class<? extends Component>, String> targetRouteMap;
    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfigureRoutes() {
        routeModel = RouteModel.create();
        routeMap = new HashMap<>();
        targetRouteMap = new HashMap<>();
        exceptionTargetMap = new HashMap<>();
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *         original configuration to get data from
     */
    public ConfigureRoutes(ConfiguredRoutes original) {
        Map<String, RouteTarget> routesMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRoutesMap = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            routesMap.put(route.getKey(), route.getValue().copy(true));
        }
        targetRoutesMap.putAll(original.getTargetRoutes());
        exceptionTargetsMap.putAll(original.getExceptionHandlers());

        this.routeModel = original.getRouteModel().clone();

        this.routeMap = routesMap;
        this.targetRouteMap = targetRoutesMap;
        this.exceptionTargetMap = exceptionTargetsMap;
    }

    /**
     * Override so that the getters use the correct routes map for data.
     *
     * @return editable map of routes
     */
    @Override
    protected Map<String, RouteTarget> getRoutesMap() {
        return routeMap;
    }

    @Override
    RouteModel getRouteModel() {
        return routeModel;
    }

    /**
     * Override so that the getters use the correct target routes map for data.
     *
     * @return editable map of targetRoutes
     */
    @Override
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return targetRouteMap;
    }

    /**
     * Override so that the getters use the correct exception targets map for
     * data.
     *
     * @return editable map of exception targets
     */
    @Override
    public Map<Class<? extends Exception>, Class<? extends Component>> getExceptionHandlers() {
        return exceptionTargetMap;
    }

    /*-----------------------------------*/
    /* Mutation functions                */
    /*-----------------------------------*/

    /**
     * Clear all maps from this configuration.
     */
    public void clear() {
        getRoutesMap().clear();
        getTargetRoutes().clear();
    }

    /**
     * Set a new {@link RouteTarget} for the given pathTemplate.
     * <p>
     * Note! this will override any previous value.
     *
     * @param pathTemplate
     *         path template for which to set route target for
     * @param navigationTarget
     *         navigation target to add
     */
    public void setRoute(String pathTemplate,
            Class<? extends Component> navigationTarget) {

        final RouteTarget target = new RouteTarget(navigationTarget);
        getRouteModel().addRoute(pathTemplate, target);

        if (!hasRouteTarget(navigationTarget)) {
            // FIXME: This seems inconsistent with the case when adding same
            // navigationTarget with different parent layouts. In that case the
            // new registered route is not an alias.
            setTargetRouteImpl(navigationTarget, pathTemplate);
        }

        getRoutesMap().put(pathTemplate, target);
    }

    /**
     * Put a new target route for Class-to-path mapping.
     * <p>
     * This is a reverse mapping to RouteTarget, which also handles any HasUrl
     * parameters, for the main route of this navigation target.
     *
     * @param navigationTarget
     *            navigation target to map
     * @param path
     *            path for given navigation target
     * 
     * @deprecated use only {@link #setRoute(String, Class)} which also handles
     *             the reverse mapping.
     */
    @Deprecated
    public void setTargetRoute(Class<? extends Component> navigationTarget,
            String path) {
        setTargetRouteImpl(navigationTarget, path);
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
        getExceptionHandlers().put(exception, errorTarget);
    }

    /**
     * Remove the targetRoute completely from the configuration.
     *
     * @param targetRoute
     *         target registered route to remove
     * @deprecated use {@link #removeTarget(Class)} instead.
     */
    @Deprecated
    public void removeRoute(Class<? extends Component> targetRoute) {
        removeTarget(targetRoute);
    }

    /**
     * Remove the target completely from the configuration.
     *
     * @param target
     *            target registered route to remove
     */
    public void removeTarget(Class<? extends Component> target) {
        if (!hasRouteTarget(target)) {
            return;
        }

        // Remove target route from class-to-string map
        getTargetRoutes().remove(target);

        List<String> emptyRoutes = new ArrayList<>();
        // Remove all instances of the route class for any path
        // that it may be registered to
        getRoutesMap().forEach((route, routeTarget) -> {
            routeTarget.remove(target);

            if (routeTarget.isEmpty()) {
                emptyRoutes.add(route);
            }
        });
        emptyRoutes.forEach(route -> {
            getRouteModel().removePath(route);
            getRoutesMap().remove(route);
        });
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

        getRouteModel().removePath(path);
        RouteTarget removedRoute = getRoutesMap().remove(path);
        removeMainRouteTarget(removedRoute.getTarget());
    }

    /**
     * Remove navigation target for given pathTemplate.
     *
     * @param pathTemplate
     *            pathTemplate to remove target from.
     * @param targetRoute
     *            target route to remove from pathTemplate.
     * @deprecated use {@link #removeRoute(String)} or
     *             {@link #removeRoute(Class)} instead.
     */
    @Deprecated
    public void removeRoute(String pathTemplate,
            Class<? extends Component> targetRoute) {
        if (!hasRoute(pathTemplate) || !getRoutesMap().get(pathTemplate)
                .containsTarget(targetRoute)) {
            return;
        }

        RouteTarget routeTarget = getRoutesMap().get(pathTemplate);
        routeTarget.remove(targetRoute);

        if (routeTarget.isEmpty()) {
            getRoutesMap().remove(pathTemplate);
        }

        if (getTargetRoutes().containsKey(targetRoute) && getTargetRoutes()
                .get(targetRoute).equals(pathTemplate)) {
            removeMainRouteTarget(targetRoute);
        }
    }

    /**
     * Update the main route target for the navigationTarget if another route
     * for the class is found.
     *
     * @param navigationTarget
     *         navigation target to update the main route for
     */
    private void removeMainRouteTarget(
            Class<? extends Component> navigationTarget) {
        getTargetRoutes().remove(navigationTarget);

        // Update Class-to-string map with a new mapping if removed route exists for another path
        for (Map.Entry<String, RouteTarget> entry : getRoutesMap().entrySet()) {
            if (entry.getValue().containsTarget(navigationTarget)) {
                setTargetRouteImpl(navigationTarget, entry.getKey());
                return;
            }
        }
    }

    private void setTargetRouteImpl(Class<? extends Component> navigationTarget,
            String pathPattern) {
        getTargetRoutes().put(navigationTarget, pathPattern);
    }

}
