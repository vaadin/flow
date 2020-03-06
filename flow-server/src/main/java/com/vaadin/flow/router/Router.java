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
package com.vaadin.flow.router;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.internal.DefaultRouteResolver;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.InternalRedirectHandler;
import com.vaadin.flow.router.internal.NavigationStateRenderer;
import com.vaadin.flow.router.internal.ResolveRequest;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * The router takes care of serving content when the user navigates within a
 * site or an application.
 *
 * @author Vaadin Ltd
 * @see Route
 * @since 1.0.
 */
public class Router implements Serializable {

    private RouteResolver routeResolver;

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

    /**
     * Enables navigation for a new UI instance. This initializes the UI content
     * based on the location used for loading the UI and sets up the UI to be
     * updated when the user navigates to some other location.
     *
     * @param ui
     *            the UI that navigation should be set up for
     * @param initRequest
     *            the Vaadin request that bootstraps the provided UI
     */
    public void initializeUI(UI ui, VaadinRequest initRequest) {
        Location location = getLocationForRequest(initRequest.getPathInfo(),
                initRequest.getParameterMap());
        initializeUI(ui, location);
    }

    /**
     * Enables navigation for a new UI instance. This initializes the UI content
     * based on the location used for loading the UI and sets up the UI to be
     * updated when the user navigates to some other location.
     *
     * @param ui
     *            the UI that navigation should be set up for
     * @param location
     *            the location object of the route
     */
    public void initializeUI(UI ui, Location location) {
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

        try {
            return new Location(path, queryParameters);
        } catch (IllegalArgumentException iae) {
            LoggerFactory.getLogger(Router.class.getName())
                    .warn("Exception when parsing location path {}", path, iae);
        }

        int index = path.indexOf('?');
        String encodedPath = path;
        if (index >= 0) {
            encodedPath = path.substring(0, index);
        }
        try {
            if (path.startsWith("/")) {
                encodedPath = URLEncoder.encode(path.substring(1),
                        StandardCharsets.UTF_8.name());
            } else {
                encodedPath = URLEncoder.encode(path,
                        StandardCharsets.UTF_8.name());
            }
        } catch (UnsupportedEncodingException e) {
            LoggerFactory.getLogger(Router.class.getName())
                    .warn("Exception when encoding path {}", path, e);
        }
        return new Location(encodedPath);
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
        return resolveNavigationTarget(location);
    }

    /**
     * Resolve the navigation target for given {@link Location} using the router
     * routeResolver.
     *
     * @param location
     *            the location object of the route
     * @return NavigationTarget for the given location if found
     */
    public Optional<NavigationState> resolveNavigationTarget(
            Location location) {
        NavigationState resolve = null;
        try {
            resolve = getRouteResolver()
                    .resolve(new ResolveRequest(this, location));
        } catch (NotFoundException nfe) {
            LoggerFactory.getLogger(Router.class.getName()).warn(
                    "Failed to resolve navigation target for path: {}",
                    location.getPath(), nfe);
        }
        return Optional.ofNullable(resolve);
    }

    /**
     * Resolve a navigation target with an empty {@link NotFoundException}.
     *
     * @return an instance of {@link NavigationState} for NotFoundException or
     *         empty if there is none in the application.
     */
    public Optional<NavigationState> resolveRouteNotFoundNavigationTarget() {
        Optional<ErrorTargetEntry> errorTargetEntry = getErrorNavigationTarget(
                new NotFoundException());
        NavigationState result = null;
        if (errorTargetEntry.isPresent()) {
            result = new NavigationStateBuilder(this)
                    .withTarget(errorTargetEntry.get().getNavigationTarget())
                    .build();
        }
        return Optional.ofNullable(result);
    }

    /**
     * Navigates the given UI to the given location.
     * <p>
     * This method just shows the given {@code location} on the page and doesn't
     * update the browser location (and page history). Use the
     * {@link UI#navigate(String, QueryParameters)} method if you want to update
     * the browser location as well.
     *
     * @param ui
     *            the UI to update, not <code>null</code>
     * @param location
     *            the location to navigate to, not <code>null</code>
     * @param trigger
     *            the type of user action that triggered this navigation, not
     *            <code>null</code>
     * @return the HTTP status code resulting from the navigation
     * @see UI#navigate(String)
     * @see UI#navigate(String, QueryParameters)
     */
    public int navigate(UI ui, Location location, NavigationTrigger trigger) {
        assert ui != null;
        assert location != null;
        assert trigger != null;
        ui.getSession().checkHasLock();

        if (handleNavigationForLocation(ui, location)) {
            ui.getInternals().setLastHandledNavigation(location);

            try {
                return handleNavigation(ui, location, trigger);
            } catch (Exception exception) {
                return handleExceptionNavigation(ui, location, exception,
                        trigger);
            } finally {
                ui.getInternals().clearLastHandledNavigation();
            }
        }
        return HttpServletResponse.SC_NOT_MODIFIED;
    }

    private boolean handleNavigationForLocation(UI ui, Location location) {
        if (ui.getInternals().hasLastHandledLocation()) {
            return !location.getPathWithQueryParameters()
                    .equals(ui.getInternals().getLastHandledLocation()
                            .getPathWithQueryParameters());
        }
        return true;
    }

    private int handleNavigation(UI ui, Location location,
            NavigationTrigger trigger) {
        NavigationState newState = getRouteResolver()
                .resolve(new ResolveRequest(this, location));
        if (newState != null) {
            NavigationEvent navigationEvent = new NavigationEvent(this,
                    location, ui, trigger);

            NavigationHandler handler = new NavigationStateRenderer(newState);
            return handler.handle(navigationEvent);
        } else if (!location.getPath().isEmpty()) {
            Location slashToggledLocation = location.toggleTrailingSlash();
            NavigationState slashToggledState = getRouteResolver()
                    .resolve(new ResolveRequest(this, slashToggledLocation));
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
    }

    private int handleExceptionNavigation(UI ui, Location location,
            Exception exception, NavigationTrigger trigger) {
        Optional<ErrorTargetEntry> maybeLookupResult = getErrorNavigationTarget(
                exception);

        if (maybeLookupResult.isPresent()) {
            ErrorTargetEntry lookupResult = maybeLookupResult.get();

            ErrorParameter<?> errorParameter = new ErrorParameter<>(
                    lookupResult.getHandledExceptionType(), exception,
                    exception.getMessage());
            ErrorStateRenderer handler = new ErrorStateRenderer(
                    new NavigationStateBuilder(this)
                            .withTarget(lookupResult.getNavigationTarget())
                            .build());

            ErrorNavigationEvent navigationEvent = new ErrorNavigationEvent(
                    this, location, ui, trigger, errorParameter);

            return handler.handle(navigationEvent);
        } else {
            throw new RuntimeException(exception);
        }
    }

    private RouteResolver getRouteResolver() {
        return routeResolver;
    }

    public RouteRegistry getRegistry() {
        // If we have a session then return the session registry
        // else return router registry
        if (VaadinSession.getCurrent() != null) {
            return SessionRouteRegistry
                    .getSessionRegistry(VaadinSession.getCurrent());
        }
        return registry;
    }

    /**
     * Get a registered navigation target for given exception.
     *
     * @param exception
     *            exception to search error view for
     * @return optional error target entry corresponding to the given exception
     */
    public Optional<ErrorTargetEntry> getErrorNavigationTarget(
            Exception exception) {
        if (registry instanceof ApplicationRouteRegistry) {
            return ((ApplicationRouteRegistry) registry)
                    .getErrorNavigationTarget(exception);
        }
        return Optional.empty();
    }
}
