/*
 * Copyright (C) 2000-2024 Vaadin Ltd
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

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on going
     */
    public AfterNavigationEvent(LocationChangeEvent event) {
        super(event.getSource());
        this.event = event;
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
}
