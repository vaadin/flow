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
package com.vaadin.client;

import java.util.function.Function;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.nodefeature.NodeFeatures;
import com.vaadin.flow.nodefeature.NodeProperties;

import elemental.dom.Element;
import elemental.events.EventRemover;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Mapping between a server side node identifier which has been requested to
 * attach existing client side element.
 *
 * @author Vaadin Ltd
 *
 */
public class ExistingElementMap {

    private final JsSet<Function<Integer, Boolean>> listeners = JsCollections
            .set();

    private final JsMap<Element, Integer> elementToId = JsCollections.map();
    // JsArray is used as a Map<Integer,Element> here. So this is a map between
    // an id and an Element.
    private final JsArray<Element> idToElement = JsCollections.array();

    private final JsSet<Double> nodeIds = JsCollections.set();

    private final JsMap<String, JsMap<String, Integer>> payloadTypes = JsCollections
            .map();

    /**
     * Gets the element stored via the {@link #add(int, Element)} method by the
     * given {@code id}.
     *
     * @param id
     *            identifier associated with an element
     * @return the element associated with the {@code id} or null if it doesn't
     *         exist
     */
    public Element getElement(int id) {
        return idToElement.get(id);
    }

    /**
     * Gets the id stored via the {@link #add(int, Element)} method by the given
     * {@code element}.
     *
     * @param element
     *            element associated with an identifier
     * @return the identifier associated with the {@code element} or null if it
     *         doesn't exist
     */
    public Integer getId(Element element) {
        return elementToId.get(element);
    }

    /**
     * Remove the identifier and the associated element from the mapping.
     *
     * @param id
     *            identifier to remove
     */
    public void remove(int id) {
        Element element = idToElement.get(id);
        if (element != null) {
            idToElement.set(id, null);
            elementToId.delete(element);

            fireRemoveEvent(id);
        }
    }

    /**
     * Adds the {@code id} and the {@code element} to the mapping.
     *
     * @param id
     *            identifier of the server side node
     * @param element
     *            element associated with the identifier
     */
    public void add(int id, Element element) {
        idToElement.set(id, element);
        elementToId.set(element, id);
    }

    /**
     * Adds the attachment information from the provided {@code node}.
     *
     * @param node
     *            the node supplied with the attachment info
     */
    public void add(StateNode node) {
        double id = node.getId();
        nodeIds.add(id);

        if (node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            JsonObject object = getPayloadObject(node);

            String type = object.getString(NodeProperties.TYPE);
            JsonValue payload = object.get(NodeProperties.PAYLOAD);

            JsMap<String, Integer> payloads = payloadTypes.get(type);
            if (payloads == null) {
                payloads = JsCollections.map();
                payloadTypes.set(type, payloads);
            }
            payloads.set(payload.toString(), node.getId());
        }
    }

    /**
     * Gets the identifier of the {@link StateNode} instance which has been
     * already added to the map with the same attachment information as
     * {@code node} has.
     *
     * @param node
     *            the node supplied with the attachment info
     * @return the identifier of the node which has been already requested to
     *         attach element for
     */
    public Integer getExistingNodeId(StateNode node) {
        if (node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            JsonObject object = getPayloadObject(node);

            String type = object.getString(NodeProperties.TYPE);
            JsonValue payload = object.get(NodeProperties.PAYLOAD);

            JsMap<String, Integer> payloads = payloadTypes.get(type);
            if (payloads != null) {
                return payloads.get(payload.toString());
            }
        }
        return null;
    }

    /**
     * Returns true is the {@code node} is in the map.
     *
     * @param node
     *            the node whose presence needs to be checked
     * @return
     */
    public boolean hasNode(StateNode node) {
        return nodeIds.has((double) node.getId());
    }

    /**
     * Removes the attachment information supplied by the {@code node}.
     *
     * @param node
     *            the node supplied with the attachment info
     */
    public void remove(StateNode node) {
        if (node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            JsonObject object = getPayloadObject(node);

            String type = object.getString(NodeProperties.TYPE);
            JsonValue payload = object.get(NodeProperties.PAYLOAD);

            JsMap<String, Integer> payloads = payloadTypes.get(type);
            if (payloads != null) {
                payloads.delete(payload.toString());
            }
        }

        if (nodeIds.delete((double) node.getId())) {
            fireRemoveEvent(node.getId());
        }
    }

    /**
     * Add remove listener for the identifier of the node.
     * <p>
     * Listener interface is a function that accepts the identifier of removed
     * node and returns {@code true} if the listener should be removed once the
     * node is removed. If it returns {@code false} then it's preserved in the
     * listeners list.
     *
     * @param listener
     *            the node remove listener to add
     * @return an event remover that can be used to remove the listener
     */
    public EventRemover addNodeRemoveListener(
            Function<Integer, Boolean> listener) {
        listeners.add(listener);
        return () -> listeners.delete(listener);
    }

    private void fireRemoveEvent(int id) {
        JsSet<Function<Integer, Boolean>> copy = JsCollections.set(listeners);

        copy.forEach(listener -> {
            if (listener.apply(id)) {
                listeners.delete(listener);
            }
        });
    }

    private JsonObject getPayloadObject(StateNode node) {
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_DATA);

        JsonObject object = (JsonObject) map.getProperty(NodeProperties.PAYLOAD)
                .getValue();
        return object;
    }
}
