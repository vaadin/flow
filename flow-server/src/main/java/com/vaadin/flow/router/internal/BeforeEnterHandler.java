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

import com.vaadin.flow.router.BeforeEnterEvent;

/**
 * The base interface for every {@link BeforeEnterEvent} handler.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeEnterHandler extends Serializable {

    /**
     * Callback executed before navigation to attaching Component chain is made.
     *
     * @param event
     *            before navigation event with event details
     */
    void beforeEnter(BeforeEnterEvent event);
}
