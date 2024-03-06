/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;

/**
 * Abstract node feature that is only present on the server. A server side
 * feature does not produce any node changes and it can't contain child nodes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class ServerSideFeature extends NodeFeature {

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public ServerSideFeature(StateNode node) {
        super(node);
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        // Server side only feature
    }

    @Override
    public void generateChangesFromEmpty() {
        // Server side only feature
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        // Server side only feature -> can't have child nodes
    }
}
