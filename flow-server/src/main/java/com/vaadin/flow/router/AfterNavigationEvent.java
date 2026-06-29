/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import com.vaadin.flow.component.HasElement;

/**
 * Event created after navigation completed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AfterNavigationEvent extends EventObject {

    private final LocationChangeEvent event;

    private final RouteParameters routeParameters;

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on going
     */
    public AfterNavigationEvent(LocationChangeEvent event) {
        this(event, RouteParameters.empty());
    }

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on going
     * @param routeParameters
     *            route parameters, not <code>null</code>
     * @since 24.3
     */
    public AfterNavigationEvent(LocationChangeEvent event,
            RouteParameters routeParameters) {
        super(event.getSource());
        this.event = event;
        this.routeParameters = routeParameters;
    }

    /**
     * Gets the new location.
     *
     * @return the new location, not {@code null}
     */
    public Location getLocation() {
        return event.getLocation();
    }

    /**
     * Get the {@link LocationChangeEvent}.
     *
     * @return the {@link LocationChangeEvent}, not {@code null}
     * @since 4.0
     */
    public LocationChangeEvent getLocationChangeEvent() {
        return event;
    }

    /**
     * Get the active chain that we have after navigation.
     *
     * @return unmodifiable list of active view chain
     */
    public List<HasElement> getActiveChain() {
        return Collections.unmodifiableList(event.getRouteTargetChain());
    }

    @Override
    public Router getSource() {
        return (Router) super.getSource();
    }

    /**
     * Check if event is for a refresh of a preserveOnRefresh view.
     *
     * @return true if refresh of a preserve on refresh view
     * @since 23.2.8
     */
    public boolean isRefreshEvent() {
        return event.getTrigger().equals(NavigationTrigger.REFRESH);
    }

    /**
     * Gets the route parameters associated with this event.
     *
     * @return route parameters retrieved from the navigation url.
     * @since 24.3
     */
    public RouteParameters getRouteParameters() {
        return routeParameters;
    }
}
