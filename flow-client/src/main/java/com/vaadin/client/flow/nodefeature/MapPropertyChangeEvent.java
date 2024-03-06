/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.nodefeature;

import com.vaadin.client.flow.reactive.ReactiveValueChangeEvent;

/**
 * Event fired when the value of a map property changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapPropertyChangeEvent extends ReactiveValueChangeEvent {

    private Object oldValue;
    private Object newValue;

    /**
     * Creates a new map property change event.
     *
     * @param source
     *            the changed map property
     * @param oldValue
     *            the old value
     * @param newValue
     *            the new value
     */
    public MapPropertyChangeEvent(MapProperty source, Object oldValue,
            Object newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the old property value.
     *
     * @return the old value
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new property value.
     *
     * @return the new value
     */
    public Object getNewValue() {
        return newValue;
    }

    @Override
    public MapProperty getSource() {
        return (MapProperty) super.getSource();
    }

}
