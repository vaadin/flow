/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.BaseJsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.internal.DefaultRouteResolver;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.InternalRedirectHandler;
import com.vaadin.flow.router.internal.NavigationStateRenderer;
import com.vaadin.flow.router.internal.ResolveRequest;
import com.vaadin.flow.server.ErrorRouteRegistry;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

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
     * @param location
     *            the location object of the route
     */
    public void initializeUI(UI ui, Location location) {
        ui.getPage().getHistory().setHistoryStateChangeHandler(e -> navigate(ui,
                e.getLocation(), e.getTrigger(), e.getState().orElse(null)));

        int statusCode = navigate(ui, location, NavigationTrigger.PAGE_LOAD);

        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            response.setStatus(statusCode);
        }
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
        Location location = new Location(pathInfo,
                QueryParameters.full(parameterMap));
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
     * Resolves the page title of the given navigation target without creating
     * an instance of it, using empty query parameters.
     *
     * @param navigationTarget
     *            the navigation target to resolve the title for, not
     *            {@code null}
     * @param routeParameters
     *            the route parameters the target is resolved with, not
     *            {@code null}
     * @return the resolved title, or an empty {@link Optional} if the target
     *         declares no title and no default generator is available
     * @see #resolvePageTitle(Class, RouteParameters, QueryParameters)
     */
    public Optional<String> resolvePageTitle(
            Class<? extends Component> navigationTarget,
            RouteParameters routeParameters) {
        return resolvePageTitle(navigationTarget, routeParameters,
                QueryParameters.empty());
    }

    /**
     * Resolves the page title of the given navigation target without creating
     * an instance of it.
     * <p>
     * The title is resolved in this order:
     * <ol>
     * <li>the per-route {@link DynamicPageTitle} generator;</li>
     * <li>the application-wide default {@link PageTitleGenerator};</li>
     * <li>the static {@link PageTitle#value()}.</li>
     * </ol>
     * The generators are obtained from the current {@link VaadinService}; when
     * no service is active, only annotation-based resolution is performed.
     * <p>
     * Since the navigation target is not instantiated,
     * {@link HasDynamicTitle#getPageTitle()} is not consulted; the title of an
     * instantiated, currently shown route may therefore differ from the result
     * of this method. This is the stateless title resolution used to render
     * navigation aids such as breadcrumbs and menus, for example over the
     * entries of
     * {@link RouteConfiguration#getRouteHierarchy(Class, RouteParameters)}.
     *
     * @param navigationTarget
     *            the navigation target to resolve the title for, not
     *            {@code null}
     * @param routeParameters
     *            the route parameters the target is resolved with, not
     *            {@code null}
     * @param queryParameters
     *            the query parameters the target is resolved with, not
     *            {@code null}
     * @return the resolved title, or an empty {@link Optional} if the target
     *         declares no title and no default generator is available
     */
    public Optional<String> resolvePageTitle(
            Class<? extends Component> navigationTarget,
            RouteParameters routeParameters, QueryParameters queryParameters) {
        VaadinService service = VaadinService.getCurrent();
        Instantiator instantiator = service != null ? service.getInstantiator()
                : null;
        PageTitle pageTitle = navigationTarget.getAnnotation(PageTitle.class);
        DynamicPageTitle dynamic = navigationTarget
                .getAnnotation(DynamicPageTitle.class);
        String value = pageTitle != null ? pageTitle.value() : "";

        PageTitleGenerator generator;
        if (dynamic != null) {
            generator = instantiatePageTitleGenerator(instantiator,
                    dynamic.value());
        } else {
            generator = instantiator != null
                    ? instantiator.getPageTitleGenerator()
                    : null;
        }
        if (generator != null) {
            return Optional.of(generator
                    .generatePageTitle(new PageTitleContext(navigationTarget,
                            routeParameters, queryParameters, value)));
        }
        return pageTitle != null ? Optional.of(value) : Optional.empty();
    }

    /**
     * Resolves the page title of the given navigation target instance, using
     * empty query parameters.
     *
     * @param navigationTarget
     *            the navigation target instance to resolve the title for, not
     *            {@code null}
     * @param routeParameters
     *            the route parameters the target is resolved with, not
     *            {@code null}
     * @return the resolved title, or an empty {@link Optional} if the target
     *         declares no title and no default generator is available
     * @see #resolvePageTitle(Component, RouteParameters, QueryParameters)
     */
    public Optional<String> resolvePageTitle(Component navigationTarget,
            RouteParameters routeParameters) {
        return resolvePageTitle(navigationTarget, routeParameters,
                QueryParameters.empty());
    }

    /**
     * Resolves the page title of the given navigation target instance.
     * <p>
     * The title is resolved in this order:
     * <ol>
     * <li>{@link HasDynamicTitle#getPageTitle()}, when the instance implements
     * {@link HasDynamicTitle} and returns a non-{@code null} title &mdash; this
     * matches the title shown when the route is actually navigated to;</li>
     * <li>the per-route {@link DynamicPageTitle} generator;</li>
     * <li>the application-wide default {@link PageTitleGenerator};</li>
     * <li>the static {@link PageTitle#value()}.</li>
     * </ol>
     * Unlike
     * {@link #resolvePageTitle(Class, RouteParameters, QueryParameters)}, which
     * cannot create an instance and therefore skips {@link HasDynamicTitle},
     * this overload mirrors the resolution performed during navigation.
     *
     * @param navigationTarget
     *            the navigation target instance to resolve the title for, not
     *            {@code null}
     * @param routeParameters
     *            the route parameters the target is resolved with, not
     *            {@code null}
     * @param queryParameters
     *            the query parameters the target is resolved with, not
     *            {@code null}
     * @return the resolved title, or an empty {@link Optional} if the target
     *         declares no title and no default generator is available
     */
    public Optional<String> resolvePageTitle(Component navigationTarget,
            RouteParameters routeParameters, QueryParameters queryParameters) {
        if (navigationTarget instanceof HasDynamicTitle hasDynamicTitle) {
            String title = hasDynamicTitle.getPageTitle();
            if (title != null) {
                return Optional.of(title);
            }
            // a null dynamic title falls through to class-based resolution,
            // matching navigation (RouteUtil.getDynamicTitle)
        }
        VaadinService service = VaadinService.getCurrent();
        Instantiator instantiator = service != null ? service.getInstantiator()
                : null;
        @SuppressWarnings("unchecked")
        Class<? extends Component> targetClass = instantiator != null
                ? (Class<? extends Component>) instantiator
                        .getApplicationClass(navigationTarget)
                : navigationTarget.getClass();
        return resolvePageTitle(targetClass, routeParameters, queryParameters);
    }

    private PageTitleGenerator instantiatePageTitleGenerator(
            Instantiator instantiator,
            Class<? extends PageTitleGenerator> generatorType) {
        return instantiator != null ? instantiator.getOrCreate(generatorType)
                : ReflectTools.createInstance(generatorType);
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
        return navigate(ui, location, trigger, (BaseJsonNode) null);
    }

    /**
     * Navigates the given UI to the given location. For internal use only.
     * <p>
     * This method pushes to the browser history if the <code>trigger</code> is
     * {@link NavigationTrigger#ROUTER_LINK} or
     * {@link NavigationTrigger#UI_NAVIGATE}.
     *
     * @param ui
     *            the UI to update, not <code>null</code>
     * @param location
     *            the location to navigate to, not <code>null</code>
     * @param trigger
     *            the type of user action that triggered this navigation, not
     *            <code>null</code>
     * @param state
     *            includes navigation state info including for example the
     *            scroll position and the complete href of the RouterLink
     * @return the HTTP status code resulting from the navigation
     * @see UI#navigate(String)
     * @see UI#navigate(String, QueryParameters)
     */
    public int navigate(UI ui, Location location, NavigationTrigger trigger,
            BaseJsonNode state) {
        return navigate(ui, location, trigger, state, false, false);
    }

    /**
     * Navigates the given UI to the given location. For internal use only.
     * <p>
     * This method pushes to the browser history if the <code>trigger</code> is
     * {@link NavigationTrigger#ROUTER_LINK} or
     * {@link NavigationTrigger#UI_NAVIGATE}.
     *
     * @param ui
     *            the UI to update, not <code>null</code>
     * @param location
     *            the location to navigate to, not <code>null</code>
     * @param trigger
     *            the type of user action that triggered this navigation, not
     *            <code>null</code>
     * @param state
     *            includes navigation state info including for example the
     *            scroll position and the complete href of the RouterLink
     * @param forceInstantiation
     *            if set to {@code true}, the navigation target will always be
     *            instantiated
     * @param recreateLayoutChain
     *            if set to {@code true}, the complete layout chain up to the
     *            navigation target will be re-instantiated. Requires
     *            {@code forceInstantiation} to be true to have an effect.
     * @return the HTTP status code resulting from the navigation
     * @see UI#navigate(String)
     * @see UI#navigate(String, QueryParameters)
     */
    public int navigate(UI ui, Location location, NavigationTrigger trigger,
            BaseJsonNode state, boolean forceInstantiation,
            boolean recreateLayoutChain) {
        assert ui != null;
        assert location != null;
        assert trigger != null;
        ui.getSession().checkHasLock();

        if (handleNavigationForLocation(ui, location)) {
            ui.getInternals().setLastHandledNavigation(location);
            try {
                return handleNavigation(ui, location, trigger, state,
                        forceInstantiation, recreateLayoutChain);
            } catch (Exception exception) {
                return handleExceptionNavigation(ui, location, exception,
                        trigger, state);
            } finally {
                ui.getInternals().clearLastHandledNavigation();
            }
        }
        return HttpStatusCode.NOT_MODIFIED.getCode();
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
            NavigationTrigger trigger, BaseJsonNode state,
            boolean forceInstantiation, boolean recreateLayoutChain) {
        NavigationState newState = getRouteResolver()
                .resolve(new ResolveRequest(this, location));

        NavigationEvent navigationEvent = null;
        NavigationHandler handler = null;

        if (newState != null) {
            navigationEvent = new NavigationEvent(this, location, ui, trigger,
                    state, false, forceInstantiation, recreateLayoutChain);

            handler = new NavigationStateRenderer(newState);
        } else if (!location.getPath().isEmpty()) {
            Location slashToggledLocation = location.toggleTrailingSlash();
            NavigationState slashToggledState = getRouteResolver()
                    .resolve(new ResolveRequest(this, slashToggledLocation));
            if (slashToggledState != null) {
                navigationEvent = new NavigationEvent(this,
                        slashToggledLocation, ui, trigger, state, false,
                        forceInstantiation, recreateLayoutChain);

                handler = new InternalRedirectHandler(slashToggledLocation);
            }
        }

        if (navigationEvent == null || handler == null) {
            throw new NotFoundException(
                    "Couldn't find route for '" + location.getPath() + "'");
        }

        return executeNavigation(ui, location, navigationEvent, handler, null);
    }

    /**
     * Render error view for exception. Finds view to render based on the
     * exception type. Exception view is chosen for matching exception and if no
     * match is found choose by extended type.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param ui
     *            current UI instance
     * @param location
     *            target location for failing navigation
     * @param exception
     *            exception thrown
     * @param trigger
     *            navigation trigger
     * @param state
     *            navigation state info
     * @return the HTTP status code to return to the client if handling an
     *         initial rendering request
     */
    public int handleExceptionNavigation(UI ui, Location location,
            Exception exception, NavigationTrigger trigger,
            BaseJsonNode state) {
        Optional<ErrorTargetEntry> maybeLookupResult = getErrorNavigationTarget(
                exception);

        if (maybeLookupResult.isEmpty()) {
            // No error target available throw runtime exception
            // this is usually only possible when routeRegistry is not
            // ApplicationRouteRegistry
            String message = String.format(
                    "No error view found for exception '%s'",
                    exception.getClass().getName());
            throw new RuntimeException(message, exception);
        }

        ErrorTargetEntry lookupResult = maybeLookupResult.get();

        ErrorParameter<?> errorParameter = new ErrorParameter<>(
                lookupResult.getHandledExceptionType(), exception,
                exception.getMessage());
        ErrorStateRenderer handler = new ErrorStateRenderer(
                new NavigationStateBuilder(this)
                        .withTarget(lookupResult.getNavigationTarget())
                        .build());

        ErrorNavigationEvent navigationEvent = new ErrorNavigationEvent(this,
                location, ui, trigger, errorParameter, state);

        try {
            return handler.handle(navigationEvent);
        } catch (Exception errorHandlingException) {
            Logger logger = LoggerFactory.getLogger(Router.class);

            // Error view threw an exception - fall back to
            // InternalServerError
            logger.error(
                    "Exception occurred while rendering error view '{}' for '{}'. "
                            + "Falling back to InternalServerError.",
                    lookupResult.getNavigationTarget(), location.getPath(),
                    errorHandlingException);

            // Render InternalServerError as fallback with original
            // exception as render error is logged
            ErrorParameter<?> fallbackParameter = new ErrorParameter<>(
                    Exception.class, exception, exception.getMessage());
            ErrorStateRenderer fallbackHandler = new ErrorStateRenderer(
                    new NavigationStateBuilder(this)
                            .withTarget(InternalServerError.class).build());

            ErrorNavigationEvent fallbackEvent = new ErrorNavigationEvent(this,
                    location, ui, trigger, fallbackParameter, state);

            // If InternalServerError also throws, let it propagate -
            // nothing more we can do
            return fallbackHandler.handle(fallbackEvent);
        }
    }

    /**
     * Execute navigation with a pre-resolved handler and optional success
     * callback.
     * <p>
     * This method handles the common navigation pattern of setting navigation
     * state, executing the handler, running a success callback, and properly
     * handling exceptions via {@link #handleExceptionNavigation}.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param ui
     *            the UI to update, not {@code null}
     * @param location
     *            the navigation location, not {@code null}
     * @param navigationEvent
     *            the navigation event to pass to the handler
     * @param handler
     *            the navigation handler to execute
     * @param onSuccess
     *            optional callback to run after successful navigation (before
     *            clearing navigation state), may be {@code null}
     * @return the HTTP status code resulting from the navigation
     */
    public int executeNavigation(UI ui, Location location,
            NavigationEvent navigationEvent, NavigationHandler handler,
            IntConsumer onSuccess) {
        ui.getInternals().setLastHandledNavigation(location);
        try {
            int result = handler.handle(navigationEvent);
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
            return result;
        } catch (Exception exception) {
            return handleExceptionNavigation(ui, location, exception,
                    navigationEvent.getTrigger(),
                    navigationEvent.getState().orElse(null));
        } finally {
            ui.getInternals().clearLastHandledNavigation();
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
        if (registry instanceof ErrorRouteRegistry) {
            return ((ErrorRouteRegistry) registry)
                    .getErrorNavigationTarget(exception);
        }
        return Optional.empty();
    }
}
