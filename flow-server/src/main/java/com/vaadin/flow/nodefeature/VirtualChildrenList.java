/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.nodefeature;

import com.vaadin.flow.StateNode;

/**
 * List of nodes describing the virtually connected child elements of an
 * element.
 *
 * @author Vaadin Ltd
 */
public class VirtualChildrenList extends StateNodeNodeList {

    /**
     * Creates a new element virtual children list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public VirtualChildrenList(StateNode node) {
        super(node);
    }

    @Override
    public void add(int index, StateNode node) {
        assert node != null;
        assert !node.hasFeature(ParentGeneratorHolder.class)
                || !node.getFeature(ParentGeneratorHolder.class)
                        .getParentGenerator().isPresent();

        super.add(index, node);
    }

    /**
     * Appends an item as last in the list.
     *
     * @param node
     *            the item to append
     */
    public void append(StateNode node) {
        add(size(), node);
    }

    @Override
    public StateNode get(int index) {
        return super.get(index);
    }

    @Override
    public StateNode remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
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
