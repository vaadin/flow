package com.vaadin.flow.router;

import java.io.Serializable;

import com.vaadin.flow.server.RouteRegistry;

/**
 * Listener that gets notified when the registered routes for a {@link
 * RouteRegistry} are changed in some way.
 *
 * @see com.vaadin.flow.router.Router#addRoutesChangedListener(RoutesChangedListener)
 */
public interface RoutesChangedListener extends Serializable {
    /**
     * Invoked when this listener receives a route changechange event from a
     * RouteRegistry.
     *
     * @param event
     *         event containing change data, not null
     */
    void routesChanged(RoutesChangedEvent event);

}
