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

/**
 * Server-side listener for client-side DOM events.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface DomEventListener extends Serializable {
    /**
     * Invoked when a DOM event has been fired.
     *
     * @param event
     *            the fired event
     */
    void handleEvent(DomEvent event);
}
