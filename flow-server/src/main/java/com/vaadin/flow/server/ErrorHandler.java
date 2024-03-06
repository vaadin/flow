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
 * Application errors handler.
 *
 * @since 1.0
 */
public interface ErrorHandler extends Serializable {

    /**
     * Invoked when an error occurs.
     *
     * @param event
     *            the fired event.
     */
    void error(ErrorEvent event);
}
