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
 * A listener that gets notified when a Vaadin service session is no longer
 * used.
 *
 * @see VaadinService#addSessionDestroyListener(SessionDestroyListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface SessionDestroyListener extends Serializable {
    /**
     * Called when a Vaadin service session is no longer used.
     *
     * @param event
     *            the event with details about the destroyed session
     */
    void sessionDestroy(SessionDestroyEvent event);
}
