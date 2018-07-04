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

import com.vaadin.flow.internal.StateNode;

/**
 * List of nodes describing the child elements of an element.
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
