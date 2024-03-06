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
 * Event fired when a property is added to a {@link NodeMap}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapPropertyAddEvent extends ReactiveValueChangeEvent {

    private MapProperty property;

    /**
     * Creates a new property add event.
     *
     * @param source
     *            the changed map
     * @param property
     *            the newly added property
     */
    public MapPropertyAddEvent(NodeMap source, MapProperty property) {
        super(source);
        this.property = property;
    }

    @Override
    public NodeMap getSource() {
        return (NodeMap) super.getSource();
    }

    /**
     * Gets the added property.
     *
     * @return the added property
     */
    public MapProperty getProperty() {
        return property;
    }

}
