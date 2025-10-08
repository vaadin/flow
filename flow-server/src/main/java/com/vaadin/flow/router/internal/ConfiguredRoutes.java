/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameterData;
import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouteParameters;

/**
 * Route configuration class that is used as a value object.
 * <p>
 * Note! This is always immutable and any changes should be made from
 * {@link ConfigureRoutes}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.3
 */
public class ConfiguredRoutes implements Serializable {

    // Stores targets accessed by urls with parameters.
    private final RouteModel routeModel;

    private final Map<String, RouteTarget> routeMap;
    private final Map<Class<? extends Component>, String> targetRouteMap;

    private final Map<Class<? extends Component>, RouteModel> targetRouteModelMap;

    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfiguredRoutes() {
        routeModel = RouteModel.create(false);
        routeMap = Collections.emptyMap();
        targetRouteMap = Collections.emptyMap();
        targetRouteModelMap = Collections.emptyMap();
        exceptionTargetMap = Collections.emptyMap();
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *            original configuration to get data from
     */
    public ConfiguredRoutes(ConfigureRoutes original) {
        Map<String, RouteTarget> originalRouteMap = new HashMap<>();
        Map<Class<? extends Component>, String> originalTargetRouteMap = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> originalExceptionTargetMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            originalRouteMap.put(route.getKey(), route.getValue());
        }
        originalTargetRouteMap.putAll(original.getTargetRoutes());
        originalExceptionTargetMap.putAll(original.getExceptionHandlers());

        Map<Class<? extends Component>, RouteModel> originalTargetRouteModelMap = original
                .copyTargetRouteModels(false);

        this.routeModel = RouteModel.copy(original.getRouteModel(), false);
        this.routeMap = originalRouteMap.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(originalRouteMap);
        this.targetRouteMap = originalTargetRouteMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(originalTargetRouteMap);
        this.targetRouteModelMap = originalTargetRouteModelMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(originalTargetRouteModelMap);
        this.exceptionTargetMap = originalExceptionTargetMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(originalExceptionTargetMap);
    }

    protected Map<String, RouteTarget> getRoutesMap() {
        return routeMap;
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
        return hasTemplate(path);
    }

    /**
     * See if configuration contains a registered route for given template.
     *
     * @param template
     *            template to check
     * @return true if configuration contains route
     */
    public boolean hasTemplate(String template) {
        return getRoutesMap().containsKey(template);
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
     *         {@link RouteTarget} and {@link RouteParameters} extracted from
     *         the <code>url</code> argument according with the route
     *         configuration.
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
        return iterateTemplates(target, template -> {
            try {
                return getRouteModel().getRouteTarget(template, parameters);
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
     * template.
     *
     * @return component-to-path map of all target routes
     */
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return targetRouteMap;
    }

    /**
     * Get all registered target routes for this configuration.
     *
     * @return component-to-path map of all target routes
     */
    Map<Class<? extends Component>, RouteModel> getTargetRouteModelMap() {
        return targetRouteModelMap;
    }

    /**
     * Make a copy of the target and route models mapping.
     *
     * @return a copy of the target and route models mapping.
     */
    protected final Map<Class<? extends Component>, RouteModel> copyTargetRouteModels(
            boolean mutable) {
        Map<Class<? extends Component>, RouteModel> copyMap = new HashMap<>();
        this.getTargetRouteModelMap().entrySet()
                .forEach(entry -> copyMap.put(entry.getKey(),
                        RouteModel.copy(entry.getValue(), mutable)));
        return copyMap;
    }

    /**
     * Get the route template String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getTargetRoute(Class<? extends Component> navigationTarget) {
        return getTemplate(navigationTarget);
    }

    /**
     * Get the route template String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getTemplate(Class<? extends Component> navigationTarget) {
        return getTargetRoutes().get(navigationTarget);
    }

    /**
     * Get the route template String for the given navigation target class and
     * using the specified parameters format.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @param format
     *            settings used to format the result parameters. If all of
     *            {@link RouteParameterFormatOption#NAME},
     *            {@link RouteParameterFormatOption#MODIFIER} and
     *            {@link RouteParameterFormatOption#REGEX} are provided, the
     *            unformatted template will be provided.
     * @return base route string if target class found
     */
    public String getTemplate(Class<? extends Component> navigationTarget,
            Set<RouteParameterFormatOption> format) {
        final String template = getTemplate(navigationTarget);
        if (template == null) {
            return null;
        }

        return getRouteModel().formatTemplate(template, format);
    }

    /**
     * Get the url path String for the given navigation target class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return route string if target class found
     */
    public String getTargetUrl(Class<? extends Component> navigationTarget) {
        return iterateTemplates(navigationTarget, template -> {
            if (RouteFormat.hasRequiredParameter(template)) {
                return null;

            } else if (RouteFormat.hasParameters(template)) {
                // In case all parameters are optional or wildcard, this will
                // return successfully.
                return getRouteModel().getUrl(template,
                        RouteParameters.empty());
            }

            return template;
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
        return iterateTemplates(navigationTarget, template -> {
            try {
                return getRouteModel().getUrl(template, parameters);
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
        return exceptionTargetMap;
    }

    /**
     * Gets the parameters defined by the given template.
     *
     * @param template
     *            template to get parameters from.
     * @return map parameter names with
     *         {@link com.vaadin.flow.router.RouteParameterData}.
     */
    public Map<String, RouteParameterData> getParameters(String template) {
        return getRouteModel().getParameters(template);
    }

    /**
     * Get the RouteTarget stored for the given template.
     *
     * @param template
     *            template to get route target for
     * @return route target for template, <code>null</code> if nothing
     *         registered
     */
    protected RouteTarget getRouteTarget(String template) {
        return getRoutesMap().get(template);
    }

    private <T> T iterateTemplates(Class<? extends Component> navigationTarget,
            Function<String, T> templateOutput) {

        final RouteModel model = getTargetRouteModelMap().get(navigationTarget);
        if (model == null) {
            return null;
        }

        for (String template : getOrderedTemplates(navigationTarget, model)) {
            final T result = templateOutput.apply(template);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Get the navigation target templates ordered so that {@link Route}
     * annotation string is first in collection.
     *
     * @param navigationTarget
     *            target class
     * @param model
     *            {@link RouteModel} for navigation target
     * @return target templates with route value before RouteAlias values
     */
    private static Collection<String> getOrderedTemplates(
            Class<? extends Component> navigationTarget, RouteModel model) {
        final Collection<String> templates = model.getRoutes().keySet();
        // No need to check route if one or no template.
        if (templates.size() <= 1) {
            return templates;
        }

        // Bring actual route to front of collection
        if (navigationTarget.isAnnotationPresent(Route.class)) {
            String route = PathUtil.trimPath(
                    navigationTarget.getAnnotation(Route.class).value());
            if (templates.contains(route)) {
                List<String> reorder = new ArrayList<>();
                templates.remove(route);
                reorder.add(route);
                reorder.addAll(templates);
                return reorder;
            }
        }
        return templates;
    }

}
