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
 * Event fired when a state node is unregistered.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NodeUnregisterEvent {
    private StateNode node;

    /**
     * Creates a new node unregister event.
     *
     * @param node
     *            the unregistered node
     */
    public NodeUnregisterEvent(StateNode node) {
        this.node = node;
    }

    /**
     * Gets the unregistered node.
     *
     * @return the unregistered node
     */
    public StateNode getNode() {
        return node;
    }
}
