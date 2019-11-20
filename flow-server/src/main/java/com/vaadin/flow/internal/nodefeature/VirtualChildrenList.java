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

import java.util.Iterator;

import com.vaadin.flow.internal.StateNode;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * List of nodes describing the virtually connected child elements of an
 * element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
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

    /**
     * Inserts an item supplied with payload data at the given index of the
     * list.
     *
     *
     * @param index
     *            index to insert at
     * @param node
     *            the item to append
     * @param type
     *            the payload type
     * @param payload
     *            the payload data
     */
    public void add(int index, StateNode node, String type, String payload) {
        add(index, node, type, payload == null ? null : Json.create(payload));
    }

    /**
     * Inserts an item supplied with payload data at the given index of the
     * list.
     *
     *
     * @param index
     *            index to insert at
     * @param node
     *            the item to append
     * @param type
     *            the payload type
     * @param payload
     *            the payload data
     */
    public void add(int index, StateNode node, String type, JsonValue payload) {
        assert node != null;

        JsonObject payloadObject = Json.createObject();
        payloadObject.put(NodeProperties.TYPE, type);
        if (payload != null) {
            payloadObject.put(NodeProperties.PAYLOAD, payload);
        }

        node.getFeature(ElementData.class).setPayload(payloadObject);
        super.add(index, node);
    }

    /**
     * Inserts an item supplied with payload type at the given index of the
     * list.
     *
     * @param index
     *            index to insert at
     * @param node
     *            the item to append
     * @param type
     *            the payload type
     */
    public void add(int index, StateNode node, String type) {
        add(index, node, type, (String) null);
    }

    /**
     * Appends an item supplied with payload data as last in the list.
     *
     * @param node
     *            the item to append
     * @param type
     *            the payload type
     * @param payload
     *            the payload data
     */
    public void append(StateNode node, String type, String payload) {
        add(size(), node, type, payload);
    }

    /**
     * Appends an item supplied with payload data as last in the list.
     *
     * @param node
     *            the item to append
     * @param type
     *            the payload type
     * @param payload
     *            the payload data
     */
    public void append(StateNode node, String type, JsonValue payload) {
        add(size(), node, type, payload);
    }

    /**
     * Appends an item supplied with payload type as last in the list.
     *
     * @param node
     *            the item to append
     * @param type
     *            the payload type
     */
    public void append(StateNode node, String type) {
        append(node, type, (String) null);
    }

    @Override
    public StateNode get(int index) {
        return super.get(index);
    }

    @Override
    public Iterator<StateNode> iterator() {
        return super.iterator();
    }

    @Override
    public int indexOf(StateNode node) {
        return super.indexOf(node);
    }

    @Override
    public StateNode remove(int index) {
        // removing the payload in case the element is reused
        get(index).getFeature(ElementData.class).remove(NodeProperties.PAYLOAD);

        // this should not omit a node change to client side.
        return super.remove(index);
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
