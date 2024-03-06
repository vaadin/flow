/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.dom.DebouncePhase;

/**
 * Debounce settings for declaratively defined client-side event handlers.
 *
 * @see DomEvent
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public @interface DebounceSettings {
    /**
     * Gets the debounce timeout to use.
     *
     * @return the debounce timeout in milliseconds, or 0 to disable debouncing.
     */
    int timeout();

    /**
     * Gets an array of debounce phases for which the event should be sent to
     * the server. There must be at least one phase.
     *
     * @see DebouncePhase
     *
     * @return an array of debounce phases
     */
    DebouncePhase[] phases();
}
