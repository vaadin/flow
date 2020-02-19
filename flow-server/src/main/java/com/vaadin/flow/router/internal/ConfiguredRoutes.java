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
import java.util.function.Function;
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

    private final Map<String, RouteTarget> urlTemplate2RouteTarget;
    private final Map<Class<? extends Component>, String> target2UrlTemplate;
    private final Map<Class<? extends Component>, List<String>> target2UrlTemplates;
    private final Map<Class<? extends Exception>, Class<? extends Component>> exception2Target;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfiguredRoutes() {
        routeModel = RouteModel.create();
        urlTemplate2RouteTarget = Collections.emptyMap();
        target2UrlTemplate = Collections.emptyMap();
        target2UrlTemplates = Collections.emptyMap();
        exception2Target = Collections.emptyMap();
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
        Map<Class<? extends Component>, List<String>> target2UrlTemplates = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            routeMap.put(route.getKey(), route.getValue().copy(false));
        }
        targetRouteMap.putAll(original.getTargetRoutes());
        target2UrlTemplates.putAll(original.getTargetUrlTemplates());
        exceptionTargetMap.putAll(original.getExceptionHandlers());

        this.routeModel = original.getRouteModel().clone();
        this.urlTemplate2RouteTarget = routeMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(routeMap);
        this.target2UrlTemplate = targetRouteMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(targetRouteMap);
        this.target2UrlTemplates = targetRouteMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(target2UrlTemplates);
        this.exception2Target = exceptionTargetMap.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(exceptionTargetMap);
    }

    protected Map<String, RouteTarget> getRoutesMap() {
        return urlTemplate2RouteTarget;
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
     * @param urlTemplate
     *            path template to check
     * @return true if configuration contains route
     * @deprecated use {@link #hasUrlTemplate(String)} instead.
     */
    @Deprecated
    public boolean hasRoute(String urlTemplate) {
        return getRoutesMap().containsKey(urlTemplate);
    }

    /**
     * See if configuration contains a registered route for given path template.
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
     * @param url
     *            path string to check
     * @param segments
     *            path segments for route
     * @return true if a route is found, else false
     * @deprecated use {@link #getNavigationRouteTarget(String)} instead.
     */
    @Deprecated
    public boolean hasRoute(String url, List<String> segments) {
        return getNavigationRouteTarget(PathUtil.getPath(url, segments))
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
     *            the navigation url used as input for searching a route target.
     * @return the result containing a valid target is found, and the url
     *         parameter values found in the <code>url</code> argument.
     */
    public NavigationRouteTarget getNavigationRouteTarget(String url) {
        return getRouteModel().getNavigationRouteTarget(url);
    }

    /**
     * Gets the {@link RouteTarget} instance matching the given target component
     * and url parameters.
     *
     * @param target
     *            a component class which is a navigation target.
     * @param parameters
     *            parameter values that may be used with given target.
     * @return the {@link RouteTarget} instance matching the given target
     *         component and url parameters.
     */
    public RouteTarget getRouteTarget(Class<? extends Component> target,
            UrlParameters parameters) {
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
            final RouteTarget routeTarget = result.getTarget();
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
     * @deprecated use {@link #getNavigationRouteTarget(String)} instead.
     */
    @Deprecated
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
     * Get all registered target routes for this configuration.
     *
     * @return component-to-path map of all target routes
     */
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return target2UrlTemplate;
    }

    /**
     * Get all registered target routes for this configuration.
     *
     * @return component-to-path map of all target routes
     */
    Map<Class<? extends Component>, List<String>> getTargetUrlTemplates() {
        return target2UrlTemplates;
    }

    /**
     * Get the route url template String for the given navigation target
     * class.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @return base route string if target class found
     */
    public String getUrlTemplate(Class<? extends Component> navigationTarget) {
        return getRouteModel().formatUrlTemplate(
                getTargetRoutes().get(navigationTarget),
                EnumSet.of(RouteParameterFormat.TEMPLATE));
    }

    /**
     * Get the route url template String for the given navigation target class
     * and using the specified parameters format.
     *
     * @param navigationTarget
     *            navigationTarget to get registered route for
     * @param format
     *            settings used to format the result parameters.
     * @return base route string if target class found
     */
    public String getUrlTemplate(Class<? extends Component> navigationTarget,
            Set<RouteParameterFormat> format) {
        final String urlTemplate = getTargetRoutes().get(navigationTarget);
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
            if (RouteFormat.hasParameters(urlTemplate)) {
                try {
                    // In case all parameters are optional, this will return
                    // successfully.
                    return getRouteModel().getUrl(urlTemplate, null);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            } else {
                return urlTemplate;
            }
        });
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
        return iterateUrlTemplates(navigationTarget,
                urlTemplate -> getRouteModel().getUrl(urlTemplate, parameters));
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
        return exception2Target;
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
     * @deprecated use {@link #getNavigationRouteTarget(String)} instead.
     */
    @Deprecated
    public List<Class<? extends RouterLayout>> getParentLayouts(String url,
            Class<? extends Component> navigationTarget) {
        final NavigationRouteTarget result = getNavigationRouteTarget(url);

        if (result.hasTarget()) {
            return result.getTarget().getParentLayouts();
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

        final String urlTemplate = getTargetRoutes().get(navigationTarget);
        RouteTarget target = null;
        if (urlTemplate != null) {
            target = getRoutesMap().get(urlTemplate);
        }

        if (target != null) {
            return target.getParentLayouts();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets the parameters defined by the given urlTemplate.
     * 
     * @param urlTemplate
     *            url template to get parameters from.
     * @return map parameter names with their types.
     */
    public Map<String, String> getParameters(String urlTemplate) {
        return getRouteModel().getParameters(urlTemplate,
                EnumSet.of(RouteParameterFormat.CAPITALIZED_TYPE));
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

        final List<String> urlTemplates = getTargetUrlTemplates()
                .get(navigationTarget);
        if (urlTemplates == null) {
            return null;
        }

        for (String urlTemplate : urlTemplates) {
            final T result = urlTemplateOutput.apply(urlTemplate);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

}
