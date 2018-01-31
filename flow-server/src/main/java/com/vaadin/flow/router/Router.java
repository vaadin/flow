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
package com.vaadin.flow.router;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.internal.DefaultRouteResolver;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.InternalRedirectHandler;
import com.vaadin.flow.router.internal.NavigationStateRenderer;
import com.vaadin.flow.router.internal.ResolveRequest;
import com.vaadin.flow.router.legacy.ImmutableRouterConfiguration;
import com.vaadin.flow.router.legacy.RouterConfiguration;
import com.vaadin.flow.router.legacy.RouterConfigurator;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.RouteRegistry;

/**
 * The router takes care of serving content when the user navigates within a
 * site or an application.
 *
 * @author Vaadin Ltd.
 *
 * @see Route
 */
public class Router implements RouterInterface {

    private RouteResolver routeResolver;

    private final RouterConfiguration configuration = new RouterConfiguration() {
        @Override
        public boolean isConfigured() {
            return registry.hasRoutes();
        }
    };

    private final RouteRegistry registry;

    /**
     * Constructs a new router with the given route registry and a
     * {@link DefaultRouteResolver}.
     *
     * @param registry
     *            the route registry to use, not <code>null</code>
     */
    public Router(RouteRegistry registry) {
        assert registry != null;
        this.registry = registry;
        routeResolver = new DefaultRouteResolver();
    }

    @Override
    public void initializeUI(UI ui, VaadinRequest initRequest) {
        Location location = getLocationForRequest(initRequest.getPathInfo(),
                initRequest.getParameterMap());

        ui.getPage().getHistory().setHistoryStateChangeHandler(
                e -> navigate(ui, e.getLocation(), e.getTrigger()));

        int statusCode = navigate(ui, location, NavigationTrigger.PAGE_LOAD);

        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            response.setStatus(statusCode);
        }
    }

    private Location getLocationForRequest(String pathInfo,
            Map<String, String[]> parameterMap) {
        final String path;
        if (pathInfo == null) {
            path = "";
        } else {
            assert pathInfo.startsWith("/");
            path = pathInfo.substring(1);
        }

        final QueryParameters queryParameters = QueryParameters
                .full(parameterMap);

        return new Location(path, queryParameters);
    }

    /**
     * Resolve the navigation target for given path and parameter map using the
     * router routeResolver.
     *
     * @param pathInfo
     *            the path relative to the application
     * @param parameterMap
     *            A mapping of parameter names to arrays of parameter values
     * @return NavigationTarget for the given path and parameter map if found
     */
    public Optional<NavigationState> resolveNavigationTarget(String pathInfo,
            Map<String, String[]> parameterMap) {
        Location location = getLocationForRequest(pathInfo, parameterMap);
        NavigationState resolve = null;
        try {
            resolve = getRouteResolver()
                    .resolve(new ResolveRequest(this, location));
        } catch (NotFoundException nfe) {
            LoggerFactory.getLogger(Router.class.getName()).warn(
                    "Failed to resolve navigation target for path: {}",
                    pathInfo, nfe);
        }
        return Optional.ofNullable(resolve);
    }

    @Override
    public int navigate(UI ui, Location location, NavigationTrigger trigger) {
        assert ui != null;
        assert location != null;
        assert trigger != null;

        try {
            NavigationState newState = getRouteResolver()
                    .resolve(new ResolveRequest(this, location));
            if (newState != null) {
                NavigationEvent navigationEvent = new NavigationEvent(this,
                        location, ui, trigger);

                NavigationHandler handler = new NavigationStateRenderer(
                        newState);
                if (notNavigatingToSameLocation(location,
                        ui.getInternals().getLastHandledLocation())) {
                    ui.getInternals().setLastHandledNavigation(location);
                    return handler.handle(navigationEvent);
                }
            }

            if (!location.getPath().isEmpty()) {
                Location slashToggledLocation = location.toggleTrailingSlash();
                NavigationState slashToggledState = getRouteResolver().resolve(
                        new ResolveRequest(this, slashToggledLocation));
                if (slashToggledState != null) {
                    NavigationEvent navigationEvent = new NavigationEvent(this,
                            slashToggledLocation, ui, trigger);

                    NavigationHandler handler = new InternalRedirectHandler(
                            slashToggledLocation);
                    return handler.handle(navigationEvent);
                }
            }

            throw new NotFoundException(
                    "Couldn't find route for '" + location.getPath() + "'");
        } catch (Exception exception) {
            ErrorParameter<?> errorParameter = new ErrorParameter<>(exception,
                    exception.getMessage());

            return navigateToExceptionView(ui, location, errorParameter);
        } finally {
            ui.getInternals().clearLastHandledNavigation();
        }
    }

    private int navigateToExceptionView(UI ui, Location location,
            ErrorParameter<?> errorParameter) {
        Optional<Class<? extends Component>> navigationTarget = getRegistry()
                .getErrorNavigationTarget(errorParameter.getException());

        if (navigationTarget.isPresent()) {
            ErrorStateRenderer handler = new ErrorStateRenderer(
                    new NavigationStateBuilder()
                            .withTarget(navigationTarget.get()).build());

            ErrorNavigationEvent navigationEvent = new ErrorNavigationEvent(
                    this, location, ui, NavigationTrigger.PROGRAMMATIC,
                    errorParameter);

            return handler.handle(navigationEvent);
        } else {
            throw new RuntimeException(errorParameter.getCustomMessage(),
                    errorParameter.getException());
        }
    }

    private boolean notNavigatingToSameLocation(Location location,
            Location lastHandledNavigation) {
        if (lastHandledNavigation != null) {
            return !location.getPathWithQueryParameters()
                    .equals(lastHandledNavigation.getPathWithQueryParameters());
        }
        return true;
    }

    @Override
    public void reconfigure(RouterConfigurator configurator) {
        // NO-OP
    }

    @Override
    public ImmutableRouterConfiguration getConfiguration() {
        return configuration;
    }

    private RouteResolver getRouteResolver() {
        return routeResolver;
    }

    /**
     * Get the registered url string for given navigation target.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @return url for the navigation target
     */
    public String getUrl(Class<? extends Component> navigationTarget) {
        String routeString = getUrlForTarget(navigationTarget);
        if (isAnnotatedParameter(navigationTarget, OptionalParameter.class,
                WildcardParameter.class)) {
            routeString = routeString.replaceAll("/\\{[\\s\\S]*}", "");
        }
        return trimRouteString(routeString);
    }

    /**
     * Trim the given route string of extra characters that can be left in
     * special cases like root target containing optional parameter.
     *
     * @param routeString
     *            route string to trim
     * @return trimmed route
     */
    private String trimRouteString(String routeString) {
        if (routeString.startsWith("/")) {
            routeString = routeString.substring(1);
        }
        return routeString;
    }

    @SafeVarargs
    private final boolean isAnnotatedParameter(
            Class<? extends Component> navigationTarget,
            Class<? extends Annotation>... parameterAnnotations) {
        for (Class<? extends Annotation> annotation : parameterAnnotations) {
            if (ParameterDeserializer.isAnnotatedParameter(navigationTarget,
                    annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the url string for given navigation target with the parameter in the
     * url.
     * <p>
     * Note! Given parameter is checked for correct class type. This means that
     * if the navigation target defined parameter is of type {@code Boolean}
     * then calling getUrl with a {@code String} will fail.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @param parameter
     *            parameter to embed into the generated url
     * @return url for the navigation target with parameter
     */
    public <T, C extends Component & HasUrlParameter<T>> String getUrl(
            Class<? extends C> navigationTarget, T parameter) {
        if (parameter == null) {
            return getUrl(navigationTarget);
        }
        return getUrl(navigationTarget, Collections.singletonList(parameter));
    }

    /**
     * Get the url string for given navigation target with the parameters in the
     * url.
     * <p>
     * Note! Given parameter is checked for correct class type. This means that
     * if the navigation target defined parameter is of type {@code Boolean}
     * then calling getUrl with a {@code String} will fail.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @param parameters
     *            parameters to embed into the generated url, not null
     * @return url for the navigation target with parameter
     */
    public <T, C extends Component & HasUrlParameter<T>> String getUrl(
            Class<? extends C> navigationTarget, List<T> parameters) {
        List<String> serializedParameters = serializeUrlParameters(
                Objects.requireNonNull(parameters));

        String routeString = getUrlForTarget(navigationTarget);

        if (!parameters.isEmpty()) {
            routeString = routeString.replace(
                    "{" + parameters.get(0).getClass().getSimpleName() + "}",
                    serializedParameters.stream()
                            .collect(Collectors.joining("/")));
        } else if (ParameterDeserializer.isAnnotatedParameter(navigationTarget,
                OptionalParameter.class)
                || ParameterDeserializer.isAnnotatedParameter(navigationTarget,
                        WildcardParameter.class)) {
            routeString = routeString.replaceAll("/\\{[\\s\\S]*}", "");
        } else {
            throw new NotFoundException(String.format(
                    "The navigation target '%s' has a non optional parameter that needs to be given.",
                    navigationTarget.getName()));
        }
        Optional<Class<? extends Component>> registryTarget = getRegistry()
                .getNavigationTarget(routeString, serializedParameters);

        if (registryTarget.isPresent()
                && !hasUrlParameters(registryTarget.get())
                && !registryTarget.get().equals(navigationTarget)) {
            throw new NotFoundException(String.format(
                    "Url matches existing navigation target '%s' with higher priority.",
                    registryTarget.get().getName()));
        }
        return trimRouteString(routeString);
    }

    private String getUrlForTarget(Class<? extends Component> navigationTarget)
            throws NotFoundException {
        Optional<String> targetUrl = getRegistry()
                .getTargetUrl(navigationTarget);
        if (!targetUrl.isPresent()) {
            throw new NotFoundException(
                    "No route found for given navigation target!");
        }
        return targetUrl.get();
    }

    private boolean hasUrlParameters(
            Class<? extends Component> navigationTarget) {
        return HasUrlParameter.class.isAssignableFrom(navigationTarget);
    }

    private <T> List<String> serializeUrlParameters(List<T> urlParameters) {
        return urlParameters.stream().filter(Objects::nonNull).map(T::toString)
                .collect(Collectors.toList());
    }

    @Override
    public RouteRegistry getRegistry() {
        return registry;
    }

    /**
     * Get all available routes.
     * 
     * @return RouteData for all registered routes
     */
    public List<RouteData> getRoutes() {
        return Collections
                .unmodifiableList(getRegistry().getRegisteredRoutes());
    }

    /**
     * Get all available routes collected by parent layout.
     * 
     * @return map of parent url to route
     */
    public Map<Class<? extends RouterLayout>, List<RouteData>> getRoutesByParent() {
        Map<Class<? extends RouterLayout>, List<RouteData>> grouped = new HashMap<>();
        for (RouteData route : getRoutes()) {
            List<RouteData> routeDataList = grouped.computeIfAbsent(
                    route.getParentLayout(), key -> new ArrayList<>());
            routeDataList.add(route);
        }

        return grouped;
    }
}
