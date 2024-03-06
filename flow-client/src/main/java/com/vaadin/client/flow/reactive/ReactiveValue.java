/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.reactive;

import elemental.events.EventRemover;

/**
 * A reactive value fires reactive value change events when its value changes
 * and registers itself as dependent on the current computation when the value
 * is accessed.
 * <p>
 * A reactive value typically uses a {@link ReactiveEventRouter} for keeping
 * track of listeners, firing events and registering the value as dependent to
 * the current computation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ReactiveValue {
    /**
     * Adds a listener that has a dependency to this value, and should be
     * notified when this value changes.
     *
     * @param reactiveValueChangeListener
     *            the listener to add
     * @return an event remover that can be used for removing the added listener
     */
    EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener);
}
