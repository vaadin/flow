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
 * List of nodes describing the child elements of an element.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementChildrenList extends StateNodeNodeList {
    /**
     * Creates a new element children list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public ElementChildrenList(StateNode node) {
        super(node);
    }

    @Override
    public void add(int index, StateNode node) {
        assert node != null;

        super.add(index, node);
    }

    @Override
    public StateNode get(int index) {
        return super.get(index);
    }

    @Override
    public StateNode remove(int index) {
        return super.remove(index);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public int indexOf(StateNode node) {
        return super.indexOf(node);
    }

    @Override
    public int size() {
        return super.size();
    }
}
