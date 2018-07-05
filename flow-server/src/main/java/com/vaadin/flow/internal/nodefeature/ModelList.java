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

import java.util.Collection;

import com.vaadin.flow.internal.StateNode;

/**
 * List for model values used in data binding in templates.
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
     * Returns <tt>true</tt> if this list contains the specified node. More
     *
     * @param node
     *            node whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified node
     */
    public boolean contains(StateNode node) {
        return indexOf(node) != -1;
    }

    @Override
    public int indexOf(StateNode item) {
        return super.indexOf(item);
    }
}
