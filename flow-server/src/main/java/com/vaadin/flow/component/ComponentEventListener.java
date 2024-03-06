/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.Serializable;
import java.util.EventListener;

/**
 * Generic listener for component events.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            component event type
 */
@FunctionalInterface
public interface ComponentEventListener<T extends ComponentEvent<?>>
        extends EventListener, Serializable {

    /**
     * Invoked when a component event has been fired.
     *
     * @param event
     *            component event
     */
    void onComponentEvent(T event);
}
