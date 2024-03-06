/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.Collection;

import com.vaadin.flow.internal.StateNode;

/**
 * List for model values used in data binding in templates.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ModelList extends StateNodeNodeList {

    /**
     * Creates an instance of this node feature.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public ModelList(StateNode node) {
        super(node);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public StateNode get(int index) {
        return super.get(index);
    }

    @Override
    public void add(StateNode item) {
        super.add(item);
    }

    @Override
    public void add(int index, StateNode item) {
        super.add(index, item);
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
    public void addAll(Collection<? extends StateNode> items) {
        super.addAll(items);
    }

    /**
     * Returns <code>true</code> if this list contains the specified node. More
     *
     * @param node
     *            node whose presence in this list is to be tested
     * @return <code>true</code> if this list contains the specified node
     */
    public boolean contains(StateNode node) {
        return indexOf(node) != -1;
    }

    @Override
    public int indexOf(StateNode item) {
        return super.indexOf(item);
    }
}
