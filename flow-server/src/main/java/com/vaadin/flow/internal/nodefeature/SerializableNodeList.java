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

import com.vaadin.flow.internal.StateNode;

/**
 * A list which contains {@link Serializable} values but not {@link StateNode}s.
 * <p>
 * For a {@link NodeList} containing {@link StateNode}s, use
 * {@link StateNodeNodeList}.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the type of Serializable objects this list contains
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class SerializableNodeList<T extends Serializable>
        extends NodeList<T> {

    /**
     * Creates a new list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    protected SerializableNodeList(StateNode node) {
        super(node);
    }

    @Override
    protected void add(int index, T item) {
        assert !(item instanceof StateNode);

        super.add(index, item);
    }

}
