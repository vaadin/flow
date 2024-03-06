/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.nodefeature;

/**
 * Listener notified when a property is added to a {@link NodeMap}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface MapPropertyAddListener {
    /**
     * Invoked when a property is added.
     *
     * @param event
     *            the property add event
     */
    void onPropertyAdd(MapPropertyAddEvent event);
}
