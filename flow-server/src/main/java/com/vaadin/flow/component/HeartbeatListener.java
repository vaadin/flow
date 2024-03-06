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

/**
 * Listener for listening to the heartbeat of the application.
 *
 * @since 2.0
 */
@FunctionalInterface
public interface HeartbeatListener extends Serializable {

    /**
     * Notifies about a heartbeat received for UI.
     *
     * @param event
     *            heartbeat event containing new value and receiving UI
     */
    void heartbeat(HeartbeatEvent event);

}
