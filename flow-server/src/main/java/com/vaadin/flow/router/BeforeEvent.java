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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.NavigationStateRenderer;

/**
 * Abstract before event class that has the common functionalities for
 * {@link BeforeLeaveEvent} and {@link BeforeEnterEvent}.
 *
 * @since 1.0
 */
public abstract class BeforeEvent extends EventObject {
    private final Location location;
    private final NavigationTrigger trigger;
    private final UI ui;

    private NavigationHandler forwardTarget;
    private NavigationHandler rerouteTarget;

    private final Class<?> navigationTarget;
    private final List<Class<? extends RouterLayout>> layouts;
    private NavigationState forwardTargetState;
    private NavigationState rerouteTargetState;
    private ErrorParameter<?> errorParameter;

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going
     * @param navigationTarget
     *            Navigation target
     * @deprecated Use {@link #BeforeEvent(NavigationEvent, Class, List)}
     *             instead.
     */
    @Deprecated
    public BeforeEvent(NavigationEvent event, Class<?> navigationTarget) {
        this(event.getSource(), event.getTrigger(), event.getLocation(),
                navigationTarget, event.getUI());

    }

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going
     * @param navigationTarget
     *            Navigation target
     * @param layouts
     *            Navigation layout chain
     */
    public BeforeEvent(NavigationEvent event, Class<?> navigationTarget,
            List<Class<? extends RouterLayout>> layouts) {
        this(event.getSource(), event.getTrigger(), event.getLocation(),
                navigationTarget, event.getUI(), layouts);

    }

    /**
     * Constructs a new BeforeNavigation Event.
     *
     * @param router
     *            the router that triggered the change, not {@code null}
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param location
     *            the new location, not {@code null}
     * @param navigationTarget
     *            navigation target class
     * @param ui
     *            the UI related to the navigation
     * @deprecated Use
     *             {@link #BeforeEvent(Router, NavigationTrigger, Location, Class, UI, List)}
     *             instead.
     */
    @Deprecated
    public BeforeEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget, UI ui) {
        this(router, trigger, location, navigationTarget, ui,
                Collections.emptyList());
    }

    /**
     * Constructs a new BeforeNavigation Event.
     *
     * @param router
     *            the router that triggered the change, not {@code null}
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param location
     *            the new location, not {@code null}
     * @param navigationTarget
     *            navigation target class
     * @param ui
     *            the UI related to the navigation
     * @param layouts
     *            the layout chain for the navigation target
     */
    public BeforeEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget, UI ui,
            List<Class<? extends RouterLayout>> layouts) {
        super(router);

        assert trigger != null;
        assert location != null;
        assert navigationTarget != null;
        assert ui != null;
        assert layouts != null;

        this.trigger = trigger;
        this.location = location;
        this.navigationTarget = navigationTarget;
        this.ui = ui;
        this.layouts = Collections.unmodifiableList(new ArrayList<>(layouts));
    }

    /**
     * Gets the new location.
     *
     * @return the new location, not {@code null}
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the type of user action that triggered this location change.
     *
     * @return the type of user action that triggered this location change, not
     *         <code>null</code>
     */
    public NavigationTrigger getTrigger() {
        return trigger;
    }

    @Override
    public Router getSource() {
        return (Router) super.getSource();
    }

    /**
     * Check if we have a forward target.
     *
     * @return forward target exists
     */
    public boolean hasForwardTarget() {
        return forwardTarget != null;
    }

    /**
     * Check if we have a reroute target.
     *
     * @return reroute target exists
     */
    public boolean hasRerouteTarget() {
        return rerouteTarget != null;
    }

    /**
     * Gets the forward target to use if the user should be forwarded to some
     * other view.
     *
     * @return navigation handler
     */
    public NavigationHandler getForwardTarget() {
        return forwardTarget;
    }

    /**
     * Gets the reroute target to use if the user should be rerouted to some
     * other view.
     *
     * @return an navigation handler
     */
    public NavigationHandler getRerouteTarget() {
        return rerouteTarget;
    }

    /**
     * Forward the navigation to use the provided navigation handler instead of
     * the currently used handler.
     *
     * @param forwardTarget
     *            the navigation handler to use, or {@code null} to clear a
     *            previously set forward target
     * @param targetState
     *            the target navigation state of the rerouting
     */
    public void forwardTo(NavigationHandler forwardTarget,
            NavigationState targetState) {
        this.forwardTargetState = targetState;
        this.forwardTarget = forwardTarget;
    }

    /**
     * Forward the navigation to the given navigation state.
     *
     * @param targetState
     *            the target navigation state, not {@code null}
     */
    public void forwardTo(NavigationState targetState) {
        Objects.requireNonNull(targetState, "targetState cannot be null");
        forwardTo(new NavigationStateRenderer(targetState), targetState);
    }

    /**
     * Forward the navigation to show the given component instead of the
     * component that is currently about to be displayed.
     *
     * @param forwardTargetComponent
     *            the component type to display, not {@code null}
     */
    public void forwardTo(Class<? extends Component> forwardTargetComponent) {
        Objects.requireNonNull(forwardTargetComponent,
                "forwardTargetComponent cannot be null");
        forwardTo(new NavigationStateBuilder(ui.getRouter())
                .withTarget(forwardTargetComponent).build());
    }

    /**
     * Forward to navigation component registered for given location string
     * instead of the component about to be displayed.
     *
     * @param location
     *            forward target location string
     */
    public void forwardTo(String location) {
        getSource().getRegistry().getNavigationTarget(location)
                .ifPresent(this::forwardTo);
    }

    /**
     * Forward to navigation component registered for given location string with
     * given location parameter instead of the component about to be displayed.
     *
     * @param location
     *            reroute target location string
     * @param locationParam
     *            location parameter
     * @param <T>
     *            location parameter type
     */
    public <T> void forwardTo(String location, T locationParam) {
        forwardTo(location, Collections.singletonList(locationParam));
    }

    /**
     * Forward to navigation component registered for given location string with
     * given location parameters instead of the component about to be displayed.
     *
     * @param location
     *            reroute target location string
     * @param locationParams
     *            location parameters
     * @param <T>
     *            location parameters type
     */
    public <T> void forwardTo(String location, List<T> locationParams) {
        forwardTo(getNavigationState(location, locationParams));
    }

    /**
     * Reroutes the navigation to use the provided navigation handler instead of
     * the currently used handler.
     *
     * @param rerouteTarget
     *            the navigation handler to use, or {@code null} to clear a
     *            previously set reroute target
     * @param targetState
     *            the target navigation state of the rerouting
     */
    public void rerouteTo(NavigationHandler rerouteTarget,
            NavigationState targetState) {
        rerouteTargetState = targetState;
        this.rerouteTarget = rerouteTarget;
    }

    /**
     * Reroutes the navigation to the given navigation state.
     *
     * @param targetState
     *            the target navigation state of the rerouting, not {@code null}
     */
    public void rerouteTo(NavigationState targetState) {
        Objects.requireNonNull(targetState, "targetState cannot be null");
        rerouteTo(new NavigationStateRenderer(targetState), targetState);
    }

    /**
     * Reroutes the navigation to show the given component instead of the
     * component that is currently about to be displayed.
     *
     * @param routeTargetType
     *            the component type to display, not {@code null}
     */
    public void rerouteTo(Class<? extends Component> routeTargetType) {
        Objects.requireNonNull(routeTargetType,
                "routeTargetType cannot be null");
        rerouteTo(new NavigationStateBuilder(ui.getRouter())
                .withTarget(routeTargetType).build());
    }

    /**
     * Reroute to navigation component registered for given location string
     * instead of the component about to be displayed.
     *
     * @param route
     *            reroute target location string
     */
    public void rerouteTo(String route) {
        getSource().getRegistry().getNavigationTarget(route)
                .ifPresent(this::rerouteTo);
    }

    /**
     * Reroute to navigation component registered for given location string with
     * given route parameter instead of the component about to be displayed.
     *
     * @param route
     *            reroute target location string
     * @param routeParam
     *            route parameter
     * @param <T>
     *            route parameter type
     */
    public <T> void rerouteTo(String route, T routeParam) {
        rerouteTo(route, Collections.singletonList(routeParam));
    }

    /**
     * Reroute to navigation component registered for given location string with
     * given route parameters instead of the component about to be displayed.
     *
     * @param route
     *            reroute target location string
     * @param routeParams
     *            route parameters
     * @param <T>
     *            route parameters type
     */
    public <T> void rerouteTo(String route, List<T> routeParams) {
        rerouteTo(getNavigationState(route, routeParams));
    }

    private Class<? extends Component> getTargetOrThrow(String route,
            List<String> segments) {
        Optional<Class<? extends Component>> target = getSource().getRegistry()
                .getNavigationTarget(route, segments);

        if (!target.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "No route '%s' accepting the parameters %s was found.",
                    route, segments));
        }
        return target.get();
    }

    private <T> void checkUrlParameterType(T routeParam,
            Class<? extends Component> target) {
        Class<?> genericInterfaceType = ReflectTools
                .getGenericInterfaceType(target, HasUrlParameter.class);
        if (!genericInterfaceType.isAssignableFrom(routeParam.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "Given route parameter '%s' is of the wrong type. Required '%s'.",
                    routeParam.getClass(), genericInterfaceType));
        }
    }

    private <T> NavigationState getNavigationState(String route,
            List<T> routeParams) {
        List<String> segments = routeParams.stream().map(Object::toString)
                .collect(Collectors.toList());
        Class<? extends Component> target = getTargetOrThrow(route, segments);

        if (!routeParams.isEmpty()) {
            checkUrlParameterType(routeParams.get(0), target);
        }

        return new NavigationStateBuilder(ui.getRouter())
                .withTarget(target, segments).build();
    }

    /**
     * Get the forward target for forwarding.
     *
     * @return forward target
     */
    public Class<? extends Component> getForwardTargetType() {
        return forwardTargetState.getNavigationTarget();
    }

    /**
     * Get the URL parameters of the forward target.
     *
     * @return URL parameters of forward target
     */
    public List<String> getForwardTargetParameters() {
        return forwardTargetState.getUrlParameters()
                .orElse(Collections.emptyList());
    }

    /**
     * Get the route target for rerouting.
     *
     * @return route target
     */
    public Class<? extends Component> getRouteTargetType() {
        return rerouteTargetState.getNavigationTarget();
    }

    /**
     * Get the navigation target.
     *
     * @return navigation target
     */
    public Class<?> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Get the layout chain for the {@link #getNavigationState(String, List)
     * navigation target}.
     * 
     * @return layout chain
     */
    public List<Class<? extends RouterLayout>> getLayouts() {
        return layouts;
    }

    /**
     * Reroute to error target for given exception without custom message.
     * <p>
     * Exception class needs to have default no-arg constructor.
     *
     * @param exception
     *            exception to get error target for
     * @see BeforeLeaveEvent#rerouteToError(Exception, String)
     */
    public void rerouteToError(Class<? extends Exception> exception) {
        rerouteToError(exception, "");
    }

    /**
     * Reroute to error target for given exception with given custom message.
     * <p>
     * Exception class needs to have default no-arg constructor.
     *
     * @param exception
     *            exception to get error target for
     * @param customMessage
     *            custom message to send to error target
     * @see BeforeLeaveEvent#rerouteToError(Exception, String)
     */
    public void rerouteToError(Class<? extends Exception> exception,
            String customMessage) {
        Exception instance = ReflectTools.createInstance(exception);
        rerouteToError(instance, customMessage);
    }

    /**
     * Reroute to error target for given exception with given custom message.
     *
     * @param exception
     *            exception to get error target for
     * @param customMessage
     *            custom message to send to error target
     */
    public void rerouteToError(Exception exception, String customMessage) {
        Optional<ErrorTargetEntry> maybeLookupResult = getSource()
                .getErrorNavigationTarget(exception);

        if (maybeLookupResult.isPresent()) {
            ErrorTargetEntry lookupResult = maybeLookupResult.get();

            rerouteTargetState = new NavigationStateBuilder(ui.getRouter())
                    .withTarget(lookupResult.getNavigationTarget()).build();
            rerouteTarget = new ErrorStateRenderer(rerouteTargetState);

            errorParameter = new ErrorParameter<>(
                    lookupResult.getHandledExceptionType(), exception,
                    customMessage);
        } else {
            throw new RuntimeException(customMessage, exception);
        }
    }

    /**
     * Check if we have an error parameter set for this navigation event.
     *
     * @return true if error parameter is set
     */
    public boolean hasErrorParameter() {
        return errorParameter != null;
    }

    /**
     * Get the set error parameter.
     *
     * @return error parameter
     */
    public ErrorParameter<?> getErrorParameter() {
        return errorParameter;
    }

    /**
     * Gets the UI this navigation takes place inside.
     *
     * @return the related UI instance
     */
    public UI getUI() {
        return ui;
    }
}
