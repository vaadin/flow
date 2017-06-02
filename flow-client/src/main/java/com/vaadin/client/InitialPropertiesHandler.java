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

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.shared.NodeFeatures;

import elemental.events.EventRemover;

/**
 * Handles server initial property values with the purpose to prevent change
 * their values from the client side.
 * <p>
 * Initial property values has to be set from the server side. Client side may
 * have default values which overrides server side values and those values are
 * sent to the server overriding server side values. This class prevents this.
 * Only properties that have not been set form the server are sent from the
 * client to the server. Properties that have been set from the server overrides
 * any client side default value.
 * 
 * @author Vaadin Ltd
 * @see StateTree#sendNodePropertySyncToServer(MapProperty)
 *
 */
public class InitialPropertiesHandler {

    private final Registry registry;

    private final JsSet<Double> newNodeDuringUpdate = JsCollections.set();

    private final JsMap<Double, JsMap<String, Object>> propertyChangesDuringUpdate = JsCollections
            .map();

    private final JsArray<MapProperty> propertyUpdateQueue = JsCollections
            .array();

    private final JsArray<EventRemover> propertyAddListeners = JsCollections
            .array();

    private final JsArray<EventRemover> propertyUpdateListeners = JsCollections
            .array();

    public InitialPropertiesHandler(Registry registry) {
        this.registry = registry;
    }

    public void flushPropertyUpdates() {
        if (!getRegistry().getStateTree().isUpdateInProgress()) {
            propertyAddListeners.forEach(EventRemover::remove);
            propertyAddListeners.clear();
            propertyUpdateListeners.forEach(EventRemover::remove);
            propertyUpdateListeners.clear();
            Reactive.addPostFlushListener(this::doFlushPropertyUpdates);
        }
    }

    public void nodeRegistered(StateNode node) {
        newNodeDuringUpdate.add(getNodeId(node));
        EventRemover remover = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .addPropertyAddListener(
                        event -> handleAddPropertyEvent(event.getProperty()));
        propertyAddListeners.push(remover);
    }

    public boolean handlePropertyUpdate(MapProperty property) {
        if (resetProperty(property)) {
            return true;
        }
        if (isNodeNewlyCreated(property.getMap().getNode())) {
            propertyUpdateQueue.push(property);
            return true;
        }
        return false;
    }

    private boolean resetProperty(MapProperty property) {
        JsMap<String, Object> ignoreProperties = propertyChangesDuringUpdate
                .get(getNodeId(property.getMap().getNode()));
        if (ignoreProperties != null
                && ignoreProperties.has(property.getName())) {
            Object value = ignoreProperties.get(property.getName());
            property.setValue(value);
            return true;
        }
        return false;
    }

    private void handleAddPropertyEvent(MapProperty property) {
        if (!getRegistry().getStateTree().isUpdateInProgress()) {
            return;
        }
        if (property.getMap().getId() != NodeFeatures.ELEMENT_PROPERTIES) {
            return;
        }
        EventRemover remover = property.addChangeListener(
                event -> handleUpdatePropertyEvent(event.getSource()));
        propertyUpdateListeners.push(remover);
        handleProperty(property);
    }

    private void handleUpdatePropertyEvent(MapProperty property) {
        if (!getRegistry().getStateTree().isUpdateInProgress()) {
            return;
        }
        handleProperty(property);
    }

    private void handleProperty(MapProperty property) {
        StateNode node = property.getMap().getNode();
        if (isNodeNewlyCreated(node)) {
            Double id = getNodeId(node);
            JsMap<String, Object> properties = propertyChangesDuringUpdate
                    .get(id);
            if (properties == null) {
                properties = JsCollections.map();
                propertyChangesDuringUpdate.set(id, properties);
            }
            properties.set(property.getName(), property.getValue());
        }
    }

    private boolean isNodeNewlyCreated(StateNode node) {
        return newNodeDuringUpdate.has(getNodeId(node));
    }

    private void doFlushPropertyUpdates() {
        newNodeDuringUpdate.clear();
        try {
            while (propertyUpdateQueue.length() > 0) {
                MapProperty property = propertyUpdateQueue.remove(0);
                if (!resetProperty(property)) {
                    getRegistry().getStateTree()
                            .sendNodePropertySyncToServer(property);
                }
            }
        } finally {
            propertyChangesDuringUpdate.clear();
        }
    }

    private Double getNodeId(StateNode node) {
        return Double.valueOf(node.getId());
    }

    private Registry getRegistry() {
        return registry;
    }

}