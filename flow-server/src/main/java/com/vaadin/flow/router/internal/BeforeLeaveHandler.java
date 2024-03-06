/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.io.Serializable;

import com.vaadin.flow.router.BeforeLeaveEvent;

/**
 * The base interface for every {@link BeforeLeaveEvent} handler.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeLeaveHandler extends Serializable {

    /**
     * Callback executed before navigation to detaching Component chain is made.
     *
     * @param event
     *            before navigation event with event details
     */
    void beforeLeave(BeforeLeaveEvent event);
}
