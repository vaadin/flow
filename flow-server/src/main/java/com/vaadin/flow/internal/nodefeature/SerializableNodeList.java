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

import java.io.Serializable;

import com.vaadin.flow.internal.StateNode;

/**
 * A list which contains {@link Serializable} values but not {@link StateNode}s.
 * <p>
 * For a {@link NodeList} containing {@link StateNode}s, use
 * {@link StateNodeNodeList}.
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
