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

import java.io.Serializable;

import com.vaadin.flow.StateNode;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * List of nodes describing the virtually connected child elements of an
 * element.
 * <p>
 * TODO: this is temporary class. It will replace {@link VirtualChildrenList}
 * (and will be renamed) along with rewriting {@code @Id} functionality.
 *
 * @author Vaadin Ltd
 *
 */
public class NewVirtualChildrenList extends StateNodeNodeList {

    /**
     * Creates a new element virtual children list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public NewVirtualChildrenList(StateNode node) {
        super(node);
    }

    public void add(int index, StateNode node, String type,
            Serializable payload) {
        assert node != null;

        JsonObject object = Json.createObject();
        node.getFeature(ElementData.class).setPyload(object);
        super.add(index, node);
    }

    public void add(int index, StateNode node, String type) {
        add(index, node, type, null);
    }

    /**
     * Appends an item as last in the list.
     *
     * @param node
     *            the item to append
     */
    public void append(StateNode node, String type, Serializable payload) {
        add(size(), node, type, payload);
    }

    /**
     * Appends an item as last in the list.
     *
     * @param node
     *            the item to append
     */
    public void append(StateNode node, String type) {
        append(node, type, null);
    }

    @Override
    protected StateNode remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return super.size();
    }

}
