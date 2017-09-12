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
package com.vaadin.flow.router.event;

import java.util.EventObject;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouterInterface;
import com.vaadin.flow.router.StaticRouteTargetRenderer;
import com.vaadin.ui.Component;

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
    private Class<? extends Component> routeTargetType;

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
     * @param routeTargetType
     *            the component type to display, not {@code null}
     */
    public void rerouteTo(NavigationHandler rerouteTarget,
            Class<? extends Component> routeTargetType) {
        this.routeTargetType = routeTargetType;
        this.rerouteTarget = rerouteTarget;
    }

    /**
     * Reroutes the navigation to show the given component instead of the
     * component that is currently about to be displayed.
     *
     * @param routeTargetType
     *            the component type to display, not {@code null}
     */
    public void rerouteTo(Class<? extends Component> routeTargetType) {
        rerouteTo(new StaticRouteTargetRenderer(routeTargetType),
                routeTargetType);
    }

    /**
     * Get the route target for rerouting.
     * 
     * @return route target
     */
    public Class<? extends Component> getRouteTargetType() {
        return routeTargetType;
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
}
