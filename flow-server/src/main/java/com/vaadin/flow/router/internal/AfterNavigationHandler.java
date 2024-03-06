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

import com.vaadin.flow.router.AfterNavigationEvent;

/**
 * The base interface for every {@link AfterNavigationEvent} handler.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface AfterNavigationHandler extends Serializable {

    /**
     * Callback executed after navigation has been executed.
     *
     * @param event
     *            after navigation event with event details
     */
    void afterNavigation(AfterNavigationEvent event);
}
