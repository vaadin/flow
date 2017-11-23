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
package com.vaadin.router.event;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.router.ContinueNavigationAction;
import com.vaadin.router.ErrorParameter;
import com.vaadin.router.ErrorStateRenderer;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.Location;
import com.vaadin.router.NavigationHandler;
import com.vaadin.router.NavigationState;
import com.vaadin.router.NavigationStateBuilder;
import com.vaadin.router.NavigationStateRenderer;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.RouterInterface;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

/**
 * Event created before navigation happens.
 *
 * @author Vaadin Ltd
 */
public class BeforeNavigationEvent extends EventObject {

    private final Location location;
    private final NavigationTrigger trigger;
    private final ActivationState activationState;

    private NavigationHandler rerouteTarget;

    private final Class<?> navigationTarget;
    private NavigationState rerouteTargetState;
    private ErrorParameter<?> errorParameter;
    private ContinueNavigationAction continueNavigationAction = null;

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on going
     * @param navigationTarget
     *            Navigation target
     * @param activationState
     *            activation state that is handled
     */
    public BeforeNavigationEvent(NavigationEvent event,
            Class<?> navigationTarget, ActivationState activationState) {
        this(event.getSource(), event.getTrigger(), event.getLocation(),
                navigationTarget, activationState);
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
     * @param activationState
     *            activation state that is handled
     */
    public BeforeNavigationEvent(RouterInterface router,
            NavigationTrigger trigger, Location location,
            Class<?> navigationTarget, ActivationState activationState) {
        super(router);

        assert trigger != null;
        assert location != null;
        assert navigationTarget != null;
        assert activationState != null : "Navigation event needs to be for deactivating or activating";

        this.trigger = trigger;
        this.location = location;
        this.navigationTarget = navigationTarget;
        this.activationState = activationState;
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
    public RouterInterface getSource() {
        return (RouterInterface) super.getSource();
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
     * Gets the reroute target to use if the user should be rerouted to some
     * other view.
     *
     * @return an navigation handler
     */
    public NavigationHandler getRerouteTarget() {
        return rerouteTarget;
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
        rerouteTo(new NavigationStateBuilder().withTarget(routeTargetType)
                .build());
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
                .ifPresent(routeTarget -> rerouteTo(routeTarget));
    }

    /**
     * Reroute to navigation component registered for given location string with
     * given route parameter instead of the component about to be displayed.
     *
     * @param route
     *            reroute target location string
     * @param routeParam
     *            route parameter
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
     */
    public <T> void rerouteTo(String route, List<T> routeParams) {
        List<String> segments = routeParams.stream().map(Object::toString)
                .collect(Collectors.toList());
        Class<? extends Component> target = getTargetOrThrow(route, segments);

        if (!routeParams.isEmpty()) {
            checkUrlParameterType(routeParams.get(0), target);
        }
        rerouteTo(new NavigationStateBuilder()
                .withTarget(target, segments).build());
    }

    private Class<? extends Component> getTargetOrThrow(String route,
            List<String> segments) {
        if (!getSource().getRegistry().hasRouteTo(route)) {
            throw new IllegalArgumentException(String.format
                    ("No navigation target found for route '%s'", route));
        }
        return getSource().getRegistry().getNavigationTarget(route, segments)
                .orElseThrow(() -> new IllegalArgumentException(String.format
                        ("The navigation target for route '%s' doesn't accept the parameters %s.",
                                route, segments)));
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

    /**
     * Get the route target for rerouting.
     *
     * @return route target
     */
    public Class<?> getRouteTargetType() {
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
     * Get current navigation state change direction.
     *
     * @return activating or deactivating
     */
    public ActivationState getActivationState() {
        return activationState;
    }

    /**
     * Reroute to error target for given exception without custom message.
     * <p>
     * Exception class needs to have default no-arg constructor.
     *
     * @param exception
     *            exception to get error target for
     * @see BeforeNavigationEvent#rerouteToError(Exception, String)
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
     * @see BeforeNavigationEvent#rerouteToError(Exception, String)
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
        Optional<Class<? extends Component>> errorNavigationTarget = getSource()
                .getRegistry().getErrorNavigationTarget(exception);

        if (errorNavigationTarget.isPresent()) {
            rerouteTargetState = new NavigationStateBuilder()
                    .withTarget(errorNavigationTarget.get()).build();
            rerouteTarget = new ErrorStateRenderer(rerouteTargetState);

            errorParameter = new ErrorParameter(exception, customMessage);
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
    public ErrorParameter getErrorParameter() {
        return errorParameter;
    }

    /**
     * Initiates the postponement of the current navigation transition,
     * allowing a listener to e.g. display a confirmation dialog before
     * finishing the transition.
     * <p>
     * This is only valid while leaving (deactivating) a page; if the method is
     * called while entering / activating the new page, it will throw an
     * {@link IllegalStateException}.
     *
     * @return the action to run when the transition is to be resumed, or null
     *
     * @throws IllegalStateException if the method is called while
     *                               entering / activating the new page
     */
    public ContinueNavigationAction postpone() throws IllegalStateException {
        if (activationState != ActivationState.DEACTIVATING) {
            throw new IllegalStateException(
                    "Transition may only be postponed in its deactivating phase");
        }
        continueNavigationAction = new ContinueNavigationAction();
        return continueNavigationAction;
    }

    /**
     * Checks whether this event was postponed.
     *
     * @return true if the event was postponed, false otherwise
     */
    public boolean isPostponed() {
        return continueNavigationAction != null;
    }

    /**
     * Gets the action used to resume this event, if it was postponed.
     *
     * @return the action used to resume this event if it was postponed,
     *         or null if it is not being postponed
     */
    public ContinueNavigationAction getContinueNavigationAction() {
        return continueNavigationAction;
    }
}
