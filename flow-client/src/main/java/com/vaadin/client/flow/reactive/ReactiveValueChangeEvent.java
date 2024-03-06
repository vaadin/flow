/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.reactive;

/**
 * Event fired when a reactive value has changed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class ReactiveValueChangeEvent {

    private ReactiveValue source;

    /**
     * Creates a new event fired from a source.
     *
     * @param source
     *            the reactive value that will fire the event
     */
    public ReactiveValueChangeEvent(ReactiveValue source) {
        this.source = source;
    }

    /**
     * Gets the reactive value from which this event originates.
     *
     * @return the event source
     */
    public ReactiveValue getSource() {
        return source;
    }
}
