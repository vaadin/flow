package com.vaadin.hummingbird.change;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

/**
 * Base class describing a change to a state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class NodeChange implements Serializable {
    private final StateNode node;

    /**
     * Creates a new change for the given node.
     * 
     * @param node
     *            the changed node
     */
    public NodeChange(StateNode node) {
        this.node = node;
    }

    /**
     * Gets the changed node.
     * 
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }
}
