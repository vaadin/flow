/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.internal.StateNode;

/**
 * Marker feature for a {@link StateNode} which is a shadow root for some
 * element.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ShadowRootHost extends ServerSideFeature {

    /**
     * Creates a new instance of the feature for the given {@code node}.
     *
     * @param node
     *            the node to create the feature for
     */
    public ShadowRootHost(StateNode node) {
        super(node);
    }

    /**
     * Gets the host state node of the shadow root node.
     *
     * @return the host element node
     */
    public StateNode getHost() {
        return getNode().getParent();
    }
}
