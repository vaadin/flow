/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Event listener that can be registered for receiving an event when a
 * {@link com.vaadin.flow.component.UI} is initialized.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface UIInitListener extends Serializable {

    /**
     * Notifies when UI is initialized .
     *
     * @param event
     *            event for the initialization
     */
    void uiInit(UIInitEvent event);
}
