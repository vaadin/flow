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
 * Event fired when a computation is invalidated.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class InvalidateEvent {
    private Computation source;

    /**
     * Creates a new event for computation.
     *
     * @param source
     *            the invalidated computation
     */
    public InvalidateEvent(Computation source) {
        this.source = source;
    }

    /**
     * Gets the invalidated computation.
     *
     * @return the invalidated computation
     */
    public Computation getSource() {
        return source;
    }

}
