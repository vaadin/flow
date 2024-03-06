/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.function.SerializableEventListener;

/**
 * Listener interface for status change events from binder.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 *
 * @see StatusChangeEvent
 * @see Binder#addStatusChangeListener(StatusChangeListener)
 */
@FunctionalInterface
public interface StatusChangeListener extends SerializableEventListener {

    /**
     * Notifies the listener about status change {@code event}.
     *
     * @param event
     *            a status change event, not null
     */
    void statusChange(StatusChangeEvent event);
}
