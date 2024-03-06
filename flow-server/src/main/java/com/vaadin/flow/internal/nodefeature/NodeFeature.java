/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.change.NodeChange;

/**
 * A node feature represents a group of related values and functionality in a
 * state node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class NodeFeature implements Serializable {
    private final StateNode node;

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public NodeFeature(StateNode node) {
        this.node = node;
    }

    /**
     * Gets the node that this feature belongs to.
     *
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Collects all changes that are recorded for this feature.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public abstract void collectChanges(Consumer<NodeChange> collector);

    /**
     * Generates all changes that would be needed to take this node from its
     * initial empty state to its current state.
     */
    public abstract void generateChangesFromEmpty();

    /**
     * Attaches an object if it is a {@link StateNode}.
     *
     * @param child
     *            the instance to maybe attach
     */
    protected void attachPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;
            childNode.setParent(getNode());
        }
    }

    /**
     * Detaches an object if it is a {@link StateNode}.
     *
     * @param child
     *            the instance to maybe detach
     */
    protected void detatchPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;

            // Should always be parent of our own children
            assert childNode.getParent() == getNode();

            childNode.setParent(null);
        }
    }

    /**
     * Passes each child node instance to the given consumer.
     *
     * @param action
     *            the consumer that accepts each child
     */
    public abstract void forEachChild(Consumer<StateNode> action);

    /**
     * Called when the state node has been attached to the state tree.
     *
     * @param initialAttach
     *            <code>true</code> if this is the first time the node is
     *            attached to a {@link StateTree}, <code>false</code> otherwise
     */
    public void onAttach(boolean initialAttach) {
        // NOOP by default
    }

    /**
     * Called when the state node has been detached from the state tree.
     */
    public void onDetach() {
        // NOOP by default
    }

    /**
     * Returns {@code true} if the underlying node may report its changes.
     * <p>
     * If its return value is {@code false} then this node should be considered
     * as "inactive" and should not send any changes to the client side at all
     * or only changes for features that disallow the changes.
     * <p>
     * Normally features don't control the node behavior so the default
     * implementation returns {@code true}. The feature which wants to control
     * the node behavior should override this method.
     *
     * @see StateNode#updateActiveState()
     *
     * @return {@code true} if the feature allows changes for the node,
     *         {@code false} otherwise
     */
    public boolean allowsChanges() {
        return true;
    }
}
