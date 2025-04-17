/*
 * Copyright 2000-2025 Vaadin Ltd.
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
     */
    public boolean isRefreshEvent() {
        return event.getTrigger().equals(NavigationTrigger.REFRESH);
    }

    /**
     * Gets the route parameters associated with this event.
     *
     * @return route parameters retrieved from the navigation url.
     */
    public RouteParameters getRouteParameters() {
        return routeParameters;
    }
}
