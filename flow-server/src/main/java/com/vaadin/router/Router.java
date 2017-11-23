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
package com.vaadin.router;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.router.ImmutableRouterConfiguration;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.router.event.ErrorNavigationEvent;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

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

        String pathInfo = initRequest.getPathInfo();

        final String path;
        if (pathInfo == null) {
            path = "";
        } else {
            assert pathInfo.startsWith("/");
            path = pathInfo.substring(1);
        }

        final QueryParameters queryParameters = QueryParameters
                .full(initRequest.getParameterMap());

        ui.getPage().getHistory().setHistoryStateChangeHandler(
                e -> navigate(ui, e.getLocation(), e.getTrigger()));

        Location location = new Location(path, queryParameters);
        int statusCode = navigate(ui, location, NavigationTrigger.PAGE_LOAD);

        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            response.setStatus(statusCode);
        }
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

    private boolean isAnnotatedParameter(
            Class<? extends Component> navigationTarget,
            Class... parameterAnnotations) {
        for (Class annotation : parameterAnnotations) {
            if (HasUrlParameter.isAnnotatedParameter(navigationTarget,
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
        return getUrl(navigationTarget, Arrays.asList(parameter));
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
        return getUrl(navigationTarget, Objects.requireNonNull(parameters),
                this::serializeUrlParameters);
    }

    /**
     * Get the url string for given navigation target with the parameters in the
     * url that are serialized using the given parameter serializer.
     * <p>
     * Note! Given parameter is checked for correct class type. This means that
     * if the navigation target defined parameter is of type {@code Boolean}
     * then calling getUrl with a {@code String} will fail.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @param parameters
     *            parameters to embed into the generated url, not null
     * @param serializer
     *            parameter serializer to use for serializing parameters list
     * @return url for the navigation target with parameter
     */
    public <T, C extends Component & HasUrlParameter<T>> String getUrl(
            Class<? extends C> navigationTarget, List<T> parameters,
            ParameterSerializer<T> serializer) {
        List<String> serializedParameters = serializer
                .serializeUrlParameters(Objects.requireNonNull(parameters));

        String routeString = getUrlForTarget(navigationTarget);

        if (!parameters.isEmpty()) {
            routeString = routeString.replace(
                    "{" + parameters.get(0).getClass().getSimpleName() + "}",
                    serializedParameters.stream()
                            .collect(Collectors.joining("/")));
        } else if (HasUrlParameter.isAnnotatedParameter(navigationTarget,
                OptionalParameter.class)
                || HasUrlParameter.isAnnotatedParameter(navigationTarget,
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
}
