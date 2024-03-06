/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;

/**
 * A list which contains {@link StateNode}s.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class StateNodeNodeList extends NodeList<StateNode> {

    /**
     * Creates a new list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    protected StateNodeNodeList(StateNode node) {
        super(node);
    }

    @Override
    protected boolean isNodeValues() {
        return true;
    }

    @Override
    protected void add(int index, StateNode item) {
        assert item != null;

        super.add(index, item);
        attachPotentialChild(item);
    }

    @Override
    protected void addAll(Collection<? extends StateNode> items) {
        super.addAll(items);
        items.forEach(this::attachPotentialChild);
    }

    @Override
    protected StateNode remove(int index) {
        StateNode removed = super.remove(index);
        detatchPotentialChild(removed);
        return removed;
    }

    @Override
    protected void clear() {
        int size = size();
        List<StateNode> children = null;
        if (size > 0) {
            children = new ArrayList<>(size);
            forEachChild(children::add);
        }
        super.clear();
        if (size > 0) {
            children.forEach(this::detatchPotentialChild);
        }
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        iterator().forEachRemaining(action);
    }
}
