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
 * Map of basic element information.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ShadowRootData extends NodeValue<StateNode> {

    /**
     * Creates a new element data map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ShadowRootData(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.SHADOW_ROOT;
    }

    public void setShadowRoot(StateNode node) {
        setValue(node);

        node.setParent(getNode());
    }

    public StateNode getShadowRoot() {
        return getValue();
    }

}
