/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;

/**
 * A list which contains {@link StateNode}s.
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
