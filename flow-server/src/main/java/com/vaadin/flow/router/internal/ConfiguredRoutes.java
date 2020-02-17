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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameterFormat;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.UrlParameters;

/**
 * Route configuration class that is used as a value object.
 * <p>
 * Note! This is always immutable and any changes should be made from
 * {@link ConfigureRoutes}.
 *
 * @since 1.3
 */
public class ConfiguredRoutes implements Serializable {

    // Stores targets accessed by urls with parameters.
    private final RouteModel routeModel;

    private final Map<String, RouteTarget> routes;
    private final Map<Class<? extends Component>, String> targetRoutes;
    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargets;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfiguredRoutes() {
        routeModel = RouteModel.create();
        routes = Collections.emptyMap();
        targetRoutes = Collections.emptyMap();
        exceptionTargets = Collections.emptyMap();
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *            original configuration to get data from
     */
    public ConfiguredRoutes(ConfiguredRoutes original) {
        Map<String, RouteTarget> routeMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRouteMap = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            routeMap.put(route.getKey(), route.getValue().copy(false));
        }
        targetRouteMap.putAll(original.getTargetRoutes());
        exceptionTargetMap.putAll(original.getExceptionHandlers());

        this.routeModel = original.getRouteModel().clone();

        // TODO: investigate this.routes = this.routeModel.getRoutes();
        this.routes = routeMap.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(routeMap);

        this.targetRoutes = targetRouteMap.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(targetRouteMap);
        this.exceptionTargets = exceptionTargetMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(exceptionTargetMap);
    }

    protected Map<String, RouteTarget> getRoutesMap() {
        return routes;
    }

    RouteModel getRouteModel() {
        return routeModel;
    }

    /*---------------------------------*/
    /* Getters and other read methods. */
    /*---------------------------------*/

    /**
     * Collect all routes for which given routeTarget is registered. This is
     * mainly for handling route aliases as reading from the class annotations
     * doesn't specifically return the actual registrations as they can change
     * during runtime.
     *
     * @param routeTarget
     *            route target to collect registered paths for
     * @return list of routes this routeTarget is registered for
     */
    protected List<String> getRoutePaths(
            Class<? extends Component> routeTarget) {
        return getRoutesMap().entrySet().stream()
                .filter(entry -> Objects.equals(routeTarget,
                        entry.getValue().getTarget()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * See if configuration contains a registered route for given path template.
     *
     * @param pathTemplate
     *            path template to check
     * @return true if configuration contains route
     * @deprecated use {@link #hasPathTemplate(String)} instead.
     */
    @Deprecated
    public boolean hasRoute(String pathTemplate) {
        return getRoutesMap().containsKey(pathTemplate);
    }

    /**
     * See if configuration contains a registered route for given path template.
     *
     * @param pathTemplate
     *            path template to check
     * @return true if configuration contains route
     */
    public boolean hasPathTemplate(String pathTemplate) {
        return getRoutesMap().containsKey(pathTemplate);
    }

    /**
     * Check if configuration holds a route for given path with possible path
     * segments.
     *
     * @param pathString
     *            path string to check
     * @param segments
     *            path segments for route
     * @return true if a route is found, else false
     * @deprecated use {@link #hasUrl(String)} instead.
     */
    @Deprecated
    public boolean hasRoute(String pathString, List<String> segments) {
        return hasUrl(PathUtil.getPath(pathString, segments));
    }

    /**
     * See if configuration matches the given path with any registered route.
     *
     * @param url
     *            url to check
     * @return true if configuration matches the given url.
     */
    public boolean hasUrl(String url) {
        return getRouteModel().getRoute(url).hasTarget();
    }

    /**
     * Check it the given route target has been registered to the configuration.
     *
     * @param target
     *            target to check registration status for
     * @return true if target is found in configuration
     */
    public boolean hasRouteTarget(Class<? extends Component> target) {
        return getTargetRoutes().containsKey(target);
    }

    /**
     * Search for a route target using given navigation <code>path</code>
     * argument.
     *
     * @param path
     *            the navigation path used as input for searching a route
     *            target.
     * @return the result containing a valid target is found, and the url
     *         parameter values found in the <code>path</code> argument.
     */
    public RouteSearchResult getRouteSearchResult(String path) {
        return getRouteModel().getRoute(path);
    }

    /**
     * Get the target class matching the given url.
     *
     * @param url
     *            string to get the route for
     * @return {@link Optional} containing the navigationTarget class if found
     */
    public Optional<Class<? extends Component>> getTarget(String url) {
        final RouteSearchResult result = getRouteModel().getRoute(url);
        if (result.hasTarget()) {
            final RouteTarget routeTarget = result.getTarget();
            return Optional.ofNullable(routeTarget.getTarget());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the route class matching the given path and path segments.
     *
     * @param pathString
     *            string to get the route for
     * @param segments
     *            possible path segments
     * @return {@link Optional} containing the navigationTarget class if found
     * @deprecated use {@link #getTarget(String)} instead.
     */
    @Deprecated
    public Optional<Class<? extends Component>> getRoute(String pathString,
            List<String> segments) {
        return getTarget(PathUtil.getPath(pathString, segments));
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
     * Get the route path simple template String for the given navigation target
     * class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getTargetRoute(Class<? extends Component> navigationTarget) {
        return getRouteModel().getRoute(getTargetRoutes().get(navigationTarget),
                EnumSet.of(RouteParameterFormat.NAME));
    }

    /**
     * Get the route path template String for the given navigation target class
     * and using the specified parameters format.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @param format
     *            settings used to format the result parameters.
     * @return base route string if target class found
     */
    public String getTargetRoute(Class<? extends Component> navigationTarget,
            EnumSet<RouteParameterFormat> format) {
        final String pathTemplate = getTargetRoutes().get(navigationTarget);
        if (pathTemplate == null) {
            return null;
        }

        return getRouteModel().getRoute(pathTemplate,
                format);
    }

    /**
     * Get the url path String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return route string if target class found
     */
    public String getTargetUrl(Class<? extends Component> navigationTarget) {
        String path = getTargetRoutes().get(navigationTarget);

        if (path == null) {
            return null;
        }

        if (RouteModel.hasParameters(path)) {
            try {
                // In case all parameters are optional, this will return
                // successfully.
                path = getRouteModel().getUrl(path, null);

            } catch (IllegalArgumentException e) {
                path = null;
            }
        }

        return path;
    }

    /**
     * Get the url path String for the given navigation target class and
     * parameters.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @param parameters
     *            url parameters
     * @return base route string if target class found
     */
    public String getTargetUrl(Class<? extends Component> navigationTarget,
            UrlParameters parameters) {

        // TODO: Feature Request:
        // implement so that in case the parameters don't match with the
        // main route, search within aliases.
        return getRouteModel().getUrl(getTargetRoutes().get(navigationTarget),
                parameters);
    }

    /**
     * Get a exception handler by exception class.
     *
     * @param exceptionClass
     *            exception class to get exception handler for
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
     * url.
     *
     * @param url
     *            url to get parent layout chain for.
     * @param navigationTarget
     *            navigation target on url to get parent layout chain for.
     * @return list of parent layout chain.
     */
    public List<Class<? extends RouterLayout>> getParentLayouts(String url,
            Class<? extends Component> navigationTarget) {
        final RouteSearchResult result = getRouteModel().getRoute(url);

        if (result.hasTarget()) {
            final RouteTarget routeTarget = result.getTarget();

            if (!Objects.equals(routeTarget.getTarget(), navigationTarget)) {
                throw new IllegalArgumentException(
                        "Invalid navigationTarget argument");
            }

            return routeTarget.getParentLayouts();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Return the parent layout chain for given navigation target.
     *
     * @param navigationTarget
     *            navigation target to get parent layout chain for.
     * @return list of parent layout chain.
     */
    public List<Class<? extends RouterLayout>> getParentLayouts(
            Class<? extends Component> navigationTarget) {

        final String pathTemplate = getTargetRoutes().get(navigationTarget);
        RouteTarget target = null;
        if (pathTemplate != null) {
            target = getRoutesMap().get(pathTemplate);
        }

        if (target != null) {
            return target.getParentLayouts();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get the RouteTarget stored for the given pathTemplate.
     *
     * @param pathTemplate
     *            pathTemplate to get route target for
     * @return route target for pathTemplate, <code>null</code> if nothing registered
     */
    protected RouteTarget getRouteTarget(String pathTemplate) {
        return getRoutesMap().get(pathTemplate);
    }


    public Map<String, String> getParameters(String pathTemplate) {
        return getRouteModel().getParameters(pathTemplate);
    }

}
