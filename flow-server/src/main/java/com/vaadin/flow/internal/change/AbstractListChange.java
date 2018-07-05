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
package com.vaadin.flow.internal.change;

import java.io.Serializable;

import com.vaadin.flow.internal.nodefeature.NodeList;

/**
 * Change describing an operation (add/remove) in a {@link NodeList list} node
 * feature.
 * 
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the items in the node list
 */
public abstract class AbstractListChange<T extends Serializable>
        extends NodeFeatureChange {

    private final int index;
    private final NodeList<T> list;

    /**
     * Creates a new list change.
     *
     * @param list
     *            the changed list
     * @param index
     *            the index of the add operations
     */
    protected AbstractListChange(NodeList<T> list, int index) {
        super(list);
        this.list = list;
        this.index = index;
    }

    /**
     * Gets the index of the change.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets a changed list.
     * 
     * @return the changed list
     */
    protected NodeList<T> getNodeList() {
        return list;
    }

    /**
     * Gets a copy of the change with the same data except {@code index}.
     * 
     * @param index
     *            the new index of the change
     * @return a copy of the change based on new index
     */
    public abstract AbstractListChange<T> copy(int index);
}
