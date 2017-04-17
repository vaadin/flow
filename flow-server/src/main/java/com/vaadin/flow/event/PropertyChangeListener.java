package com.vaadin.flow.event;

import java.io.Serializable;

import com.vaadin.flow.dom.EventRegistrationHandle;

/**
 * A listener for property change events.
 *
 * @see PropertyChangeEvent
 * @see EventRegistrationHandle
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
    public void propertyChange(PropertyChangeEvent event);
}
