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
 * Listener notified when the value of a {@link MapProperty} changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface MapPropertyChangeListener {
    /**
     * Invoked when the value changes.
     *
     * @param event
     *            the property change event
     */
    void onPropertyChange(MapPropertyChangeEvent event);
}
