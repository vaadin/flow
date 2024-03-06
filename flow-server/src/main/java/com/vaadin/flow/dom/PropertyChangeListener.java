/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * A listener for property change events.
 *
 * @see PropertyChangeEvent
 * @see Registration
 * @since 1.0
 */
@FunctionalInterface
public interface PropertyChangeListener extends Serializable {
    /**
     * Invoked when this listener receives a property change event from an event
     * source to which it has been added.
     *
     * @param event
     *            the received event, not null
     */
    void propertyChange(PropertyChangeEvent event);
}
