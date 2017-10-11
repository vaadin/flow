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

import java.util.Arrays;
import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;

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
        Optional<Class<? extends Component>> optionalTarget = getSource()
                .getRegistry().getNavigationTarget(route,
                        Arrays.asList(routeParam.toString()));

        if (optionalTarget.isPresent()) {
            boolean hasUrlParameter = HasUrlParameter.class
                    .isAssignableFrom(optionalTarget.get());
            if (hasUrlParameter) {
                Class genericInterfaceType = ReflectTools
                        .getGenericInterfaceType(optionalTarget.get(),
                                HasUrlParameter.class);
                if (genericInterfaceType
                        .isAssignableFrom(routeParam.getClass())) {
                    rerouteTo(
                            new NavigationStateBuilder()
                                    .withTarget(optionalTarget.get(),
                                            Arrays.asList(
                                                    routeParam.toString()))
                                    .build());
                } else {
                    throw new IllegalArgumentException(String.format(
                            "Given route parameter '%s' is of the wrong type. Required '%s'.",
                            routeParam.getClass(), genericInterfaceType));
                }
            } else {
                throw new IllegalArgumentException(String.format(
                        "Found navigation target for route '%s' doesn't support url parameters.",
                        route));
            }
        } else {
            throw new IllegalArgumentException(
                    "No navigation target found route '" + route + "'");
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
     * 
     * @param exception
     *            exception to get error target for
     */
    public void rerouteToError(Class<? extends Exception> exception) {
        rerouteToError(exception, "");
    }

    /**
     * Reroute to error target for given exception with given custom message.
     * 
     * @param exception
     *            exception to get error target for
     * @param customMessage
     *            custom message to send to error target
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
        ErrorParameter<?> errorParameter = new ErrorParameter(exception,
                customMessage);

        Optional<Class<? extends Component>> navigationTarget = getSource()
                .getRegistry()
                .getErrorNavigationTarget(errorParameter.getException());

        if (navigationTarget.isPresent()) {
            rerouteTargetState = new NavigationStateBuilder()
                    .withTarget(navigationTarget.get()).build();
            rerouteTarget = new ErrorStateRenderer(rerouteTargetState);

            this.errorParameter = errorParameter;
        } else {
            throw new RuntimeException(customMessage,
                    errorParameter.getException());
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
}
