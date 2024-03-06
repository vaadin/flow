/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;

import com.vaadin.flow.server.RouteRegistry;

/**
 * Listener that gets notified when the registered routes for a
 * {@link RouteRegistry} are changed in some way.
 *
 * @since 1.3
 *
 * @see RouteRegistry#addRoutesChangeListener(RoutesChangedListener)
 */
@FunctionalInterface
public interface RoutesChangedListener extends Serializable {
    /**
     * Invoked when this listener receives a route change event from a
     * RouteRegistry.
     *
     * @param event
     *            event containing change data, not null
     */
    void routesChanged(RoutesChangedEvent event);

}
