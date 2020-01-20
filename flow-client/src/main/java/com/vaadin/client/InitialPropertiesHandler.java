/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.TreeChangeProcessor;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

/**
 * Handles server initial property values with the purpose to prevent change
 * their values from the client side.
 * <p>
 * Initial property values have to be set from the server side. Client side may
 * have default values which override server side values and those values are
 * sent to the server (overriding server side values). This class prevents this.
 * Only properties that have not been set form the server are sent from the
 * client to the server. Properties that have been set from the server overrides
 * any client side default value.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @see StateTree#sendNodePropertySyncToServer(MapProperty)
 *
 */
public class InitialPropertiesHandler {

    private final Registry registry;

    private final JsSet<Double> newNodeDuringUpdate = JsCollections.set();

    private final JsArray<MapProperty> propertyUpdateQueue = JsCollections
            .array();

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public InitialPropertiesHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Flushes collected property update queue (requested to be sent from the
     * client to the server).
     * <p>
     * Supposed to be called in the end of {@link TreeChangeProcessor} changes
     * processing.
     */
    public void flushPropertyUpdates() {
        if (!getRegistry().getStateTree().isUpdateInProgress()) {
            JsMap<Double, JsMap<String, Object>> map = JsCollections.map();
            newNodeDuringUpdate
                    .forEach(node -> collectInitialProperties(node, map));
            Reactive.addPostFlushListener(() -> doFlushPropertyUpdates(map));
        }
    }

    /**
     * Notifies the handler about registered node.
     * <p>
     * The method is called for the newly created {@code node} which is
     * registered in the {@code StateTree}.
     *
     * @param node
     *            the registered node
     */
    public void nodeRegistered(StateNode node) {
        newNodeDuringUpdate.add(getNodeId(node));
    }

    /**
     * Handles {@code property} update request before it's sent to the server
     * via RPC.
     * <p>
     * The method returns {@code true} for the {@code property} which shouldn't
     * be sent to the server because it's going to be handled by the handler
     * (queued and sent later on if allowed).
     *
     * @param property
     *            property to handle
     * @return {@code true} if property is handled by the handler, {@code false}
     *         otherwise
     */
    public boolean handlePropertyUpdate(MapProperty property) {
        if (isNodeNewlyCreated(property.getMap().getNode())) {
            propertyUpdateQueue.push(property);
            return true;
        }
        return false;
    }

    private boolean resetProperty(MapProperty property,
            JsMap<Double, JsMap<String, Object>> properties) {
        JsMap<String, Object> ignoreProperties = properties
                .get(getNodeId(property.getMap().getNode()));
        if (ignoreProperties != null
                && ignoreProperties.has(property.getName())) {
            Object value = ignoreProperties.get(property.getName());
            property.setValue(value);
            return true;
        }
        return false;
    }

    private boolean isNodeNewlyCreated(StateNode node) {
        return newNodeDuringUpdate.has(getNodeId(node));
    }

    private void doFlushPropertyUpdates(
            JsMap<Double, JsMap<String, Object>> properties) {
        newNodeDuringUpdate.clear();
        while (propertyUpdateQueue.length() > 0) {
            MapProperty property = propertyUpdateQueue.remove(0);
            if (!resetProperty(property, properties)) {
                getRegistry().getStateTree()
                        .sendNodePropertySyncToServer(property);
            }
            /*
             * Do flush after each property update. There may be several
             * properties and it looks like a property update may trigger
             * default values of other properties back.
             *
             * See https://github.com/vaadin/flow/issues/2304
             */
            Reactive.flush();
        }
    }

    private Double getNodeId(StateNode node) {
        return Double.valueOf(node.getId());
    }

    private Registry getRegistry() {
        return registry;
    }

    private void collectInitialProperties(Double id,
            JsMap<Double, JsMap<String, Object>> properties) {
        StateNode node = getRegistry().getStateTree().getNode(id.intValue());
        if (node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
            JsMap<String, Object> map = JsCollections.map();
            node.getMap(NodeFeatures.ELEMENT_PROPERTIES).forEachProperty(
                    (property, name) -> map.set(name, property.getValue()));
            properties.set(id, map);
        }
    }
}
