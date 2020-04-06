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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameterData;
import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouteParameters;

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

    private final Map<String, RouteTarget> pathToTarget;
    private final Map<Class<? extends Component>, String> targetToPath;

    private final Map<Class<? extends Component>, RouteModel> targetRouteModels;

    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionToTarget;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfiguredRoutes() {
        routeModel = RouteModel.create(false);
        pathToTarget = Collections.emptyMap();
        targetToPath = Collections.emptyMap();
        targetRouteModels = Collections.emptyMap();
        exceptionToTarget = Collections.emptyMap();
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *            original configuration to get data from
     */
    public ConfiguredRoutes(ConfigureRoutes original) {
        Map<String, RouteTarget> routeMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRouteMap = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            routeMap.put(route.getKey(), route.getValue());
        }
        targetRouteMap.putAll(original.getTargetRoutes());
        exceptionTargetMap.putAll(original.getExceptionHandlers());

        Map<Class<? extends Component>, RouteModel> target2UrlTemplatesMap = original
                .copyTargetRouteModels(false);

        this.routeModel = RouteModel.copy(original.getRouteModel(), false);
        this.pathToTarget = routeMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(routeMap);
        this.targetToPath = targetRouteMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(targetRouteMap);
        this.targetRouteModels = target2UrlTemplatesMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(target2UrlTemplatesMap);
        this.exceptionToTarget = exceptionTargetMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(exceptionTargetMap);
    }

    protected Map<String, RouteTarget> getRoutesMap() {
        return pathToTarget;
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
                .filter(entry -> entry.getValue().containsTarget(routeTarget))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * See if configuration contains a registered route for given path.
     *
     * @param path
     *            path to check
     * @return true if configuration contains route
     */
    public boolean hasRoute(String path) {
        return hasUrlTemplate(path);
    }

    /**
     * See if configuration contains a registered route for given url template.
     *
     * @param urlTemplate
     *            url template to check
     * @return true if configuration contains route
     */
    public boolean hasUrlTemplate(String urlTemplate) {
        return getRoutesMap().containsKey(urlTemplate);
    }

    /**
     * Check if configuration holds a route for given path with possible path
     * segments.
     *
     * @param path
     *            path string to check
     * @param segments
     *            path segments for route
     * @return true if a route is found, else false
     */
    public boolean hasRoute(String path, List<String> segments) {
        return getNavigationRouteTarget(PathUtil.getPath(path, segments))
                .hasTarget();
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
     * Search for a route target using given navigation <code>url</code>
     * argument.
     *
     * @param url
     *            the navigation url used to search a route target.
     * @return a {@link NavigationRouteTarget} instance containing the
     *         {@link RouteTarget} and {@link RouteParameters} extracted from the
     *         <code>url</code> argument according with the route configuration.
     */
    public NavigationRouteTarget getNavigationRouteTarget(String url) {
        return getRouteModel().getNavigationRouteTarget(url);
    }

    /**
     * Gets the {@link RouteTarget} instance matching the given target component
     * and route parameters.
     *
     * @param target
     *            a component class which is a navigation target.
     * @param parameters
     *            parameter values that may be used with given target.
     * @return the {@link RouteTarget} instance matching the given target
     *         component and route parameters.
     */
    public RouteTarget getRouteTarget(Class<? extends Component> target,
                                      RouteParameters parameters) {
        return iterateUrlTemplates(target, urlTemplate -> {
            try {
                return getRouteModel().getRouteTarget(urlTemplate, parameters);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    /**
     * Get the target class matching the given url.
     *
     * @param url
     *            string to get the route for
     * @return {@link Optional} containing the navigationTarget class if found
     */
    public Optional<Class<? extends Component>> getTarget(String url) {
        final NavigationRouteTarget result = getNavigationRouteTarget(url);
        if (result.hasTarget()) {
            final RouteTarget routeTarget = result.getRouteTarget();
            return Optional.ofNullable(routeTarget.getTarget());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the route class matching the given path and path segments.
     *
     * @param url
     *            string to get the route for
     * @param segments
     *            possible path segments
     * @return {@link Optional} containing the navigationTarget class if found
     */
    public Optional<Class<? extends Component>> getRoute(String url,
            List<String> segments) {
        return getTarget(PathUtil.getPath(url, segments));
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
     * Get all registered target routes for this configuration mapping the main
     * url template.
     *
     * @return component-to-path map of all target routes
     */
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return targetToPath;
    }

    /**
     * Get all registered target routes for this configuration.
     *
     * @return component-to-path map of all target routes
     */
    Map<Class<? extends Component>, RouteModel> getTargetRouteModels() {
        return targetRouteModels;
    }

    /**
     * Make a copy of the target and route models mapping.
     * 
     * @return a copy of the target and route models mapping.
     */
    protected final Map<Class<? extends Component>, RouteModel> copyTargetRouteModels(
            boolean mutable) {
        Map<Class<? extends Component>, RouteModel> copyMap = new HashMap<>();
        this.getTargetRouteModels().entrySet().forEach(entry -> copyMap
                .put(entry.getKey(), RouteModel.copy(entry.getValue(), mutable)));
        return copyMap;
    }

    /**
     * Get the route url template String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getTargetRoute(Class<? extends Component> navigationTarget) {
        return getUrlTemplate(navigationTarget);
    }

    /**
     * Get the route url template String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getUrlTemplate(Class<? extends Component> navigationTarget) {
        return getTargetRoutes().get(navigationTarget);
    }

    /**
     * Get the route url template String for the given navigation target class
     * and using the specified parameters format.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @param format
     *            settings used to format the result parameters. If all of
     *            {@link RouteParameterFormatOption#NAME},
     *            {@link RouteParameterFormatOption#MODIFIER} and
     *            {@link RouteParameterFormatOption#REGEX} are provided, the
     *            unformatted url template will be provided.
     * @return base route string if target class found
     */
    public String getUrlTemplate(Class<? extends Component> navigationTarget,
            Set<RouteParameterFormatOption> format) {
        final String urlTemplate = getUrlTemplate(navigationTarget);
        if (urlTemplate == null) {
            return null;
        }

        return getRouteModel().formatUrlTemplate(urlTemplate, format);
    }

    /**
     * Get the url path String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return route string if target class found
     */
    public String getTargetUrl(Class<? extends Component> navigationTarget) {
        return iterateUrlTemplates(navigationTarget, urlTemplate -> {
            if (RouteFormat.hasRequiredParameter(urlTemplate)) {
                return null;

            } else if (RouteFormat.hasParameters(urlTemplate)) {
                // In case all parameters are optional or wildcard, this will
                // return successfully.
                return getRouteModel().getUrl(urlTemplate,
                        RouteParameters.empty());
            }

            return urlTemplate;
        });
    }

    /**
     * Get the url path String for the given navigation target class and
     * parameters.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @param parameters
     *            route parameters
     * @return url String populated with parameters for the given
     *         navigationTarget
     */
    public String getTargetUrl(Class<? extends Component> navigationTarget,
            RouteParameters parameters) {
        return iterateUrlTemplates(navigationTarget, urlTemplate -> {
            try {
                return getRouteModel().getUrl(urlTemplate, parameters);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
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
        return exceptionToTarget;
    }

    /**
     * Return the parent layout chain for given navigation target on the target
     * path.
     *
     * @param path
     *            path to get parent layout chain for.
     * @param navigationTarget
     *            navigation target on path to get parent layout chain for.
     * @return list of parent layout chain.
     * @deprecated use {@link #getNavigationRouteTarget(String)} instead.
     */
    @Deprecated
    public List<Class<? extends RouterLayout>> getParentLayouts(String path,
            Class<? extends Component> navigationTarget) {
        final NavigationRouteTarget result = getNavigationRouteTarget(path);

        if (result.hasTarget()) {
            return result.getRouteTarget().getParentLayouts();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets the parameters defined by the given urlTemplate.
     * 
     * @param urlTemplate
     *            url template to get parameters from.
     * @return map parameter names with {@link com.vaadin.flow.router.RouteParameterData}.
     */
    public Map<String, RouteParameterData> getParameters(String urlTemplate) {
        return getRouteModel().getParameters(urlTemplate);
    }

    /**
     * Get the RouteTarget stored for the given urlTemplate.
     *
     * @param urlTemplate
     *            urlTemplate to get route target for
     * @return route target for urlTemplate, <code>null</code> if nothing
     *         registered
     */
    protected RouteTarget getRouteTarget(String urlTemplate) {
        return getRoutesMap().get(urlTemplate);
    }

    private <T> T iterateUrlTemplates(
            Class<? extends Component> navigationTarget,
            Function<String, T> urlTemplateOutput) {

        final RouteModel model = getTargetRouteModels()
                .get(navigationTarget);
        if (model == null) {
            return null;
        }

        final Collection<String> urlTemplates = model.getRoutes().keySet();
        for (String urlTemplate : urlTemplates) {
            final T result = urlTemplateOutput.apply(urlTemplate);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

}
