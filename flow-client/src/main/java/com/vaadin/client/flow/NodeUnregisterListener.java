/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

/**
 * A listener that will be notified when a state node is unregistered.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface NodeUnregisterListener {
    /**
     * Invoked when a state node is unregistered.
     *
     * @param event
     *            the node unregister event
     */
    void onUnregister(NodeUnregisterEvent event);
}
