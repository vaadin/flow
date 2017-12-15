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
package com.vaadin.flow.server.communication.rpc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.JsonCodec;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.nodefeature.ModelList;
import com.vaadin.flow.nodefeature.NodeFeature;
import com.vaadin.flow.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.nodefeature.NodeMap;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Model map synchronization RPC handler.
 *
 * @see JsonConstants#RPC_TYPE_MAP_SYNC
 *
 * @author Vaadin Ltd
 *
 */
public class MapSyncRpcHandler extends AbstractRpcInvocationHandler {

    private List<Runnable> pendingChangeEvents = new ArrayList<>();

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_MAP_SYNC;
    }

    @Override
    protected void handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_FEATURE);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY_VALUE);

        int featureId = (int) invocationJson
                .getNumber(JsonConstants.RPC_FEATURE);
        Class<? extends NodeFeature> feature = NodeFeatureRegistry
                .getFeature(featureId);
        assert NodeMap.class.isAssignableFrom(feature);

        String property = invocationJson.getString(JsonConstants.RPC_PROPERTY);
        Serializable value = JsonCodec.decodeWithoutTypeInfo(
                invocationJson.get(JsonConstants.RPC_PROPERTY_VALUE));

        value = tryConvert(value, node);

        ElementPropertyMap elementPropertyMap = (ElementPropertyMap) node
                .getFeature(feature);
        Runnable changeEventRunnable = elementPropertyMap
                .deferredUpdateFromClient(property, value);
        pendingChangeEvents.add(changeEventRunnable);
    }

    /**
     * Triggers and clears all pending property change events that have been
     * accumulated during the handling of nodes.
     */
    public void flushPendingChangeEvents() {
        pendingChangeEvents.forEach(Runnable::run);
        pendingChangeEvents.clear();
    }

    private Serializable tryConvert(Serializable value, StateNode context) {
        if (value instanceof JsonObject) {
            JsonObject json = (JsonObject) value;
            if (json.hasKey("nodeId")) {
                StateTree tree = (StateTree) context.getOwner();
                double id = json.getNumber("nodeId");
                StateNode stateNode = tree.getNodeById((int) id);
                return tryCopyStateNode(stateNode, json);
            }
        }
        return value;
    }

    private Serializable tryCopyStateNode(StateNode node,
            JsonObject properties) {
        if (node == null) {
            return properties;
        }

        // Copy only if the request is for a node inside a list
        if (isInList(node)) {
            StateNode copy = new StateNode(node);
            ElementPropertyMap originalProperties = node
                    .getFeature(ElementPropertyMap.class);
            ElementPropertyMap copyProperties = copy
                    .getFeature(ElementPropertyMap.class);
            originalProperties.getPropertyNames()
                    .forEach(property -> copyProperties.setProperty(property,
                            originalProperties.getProperty(property)));
            return copy;
        }
        if (isProperty(node)) {
            return node;
        }
        return properties;
    }

    private boolean isProperty(StateNode node) {
        StateNode parent = node.getParent();
        assert parent != null;
        if (parent.hasFeature(ElementPropertyMap.class)) {
            ElementPropertyMap map = parent
                    .getFeature(ElementPropertyMap.class);
            return map.getPropertyNames()
                    .anyMatch(name -> node.equals(map.getProperty(name)));
        }
        return false;
    }

    private boolean isInList(StateNode node) {
        StateNode parent = node.getParent();
        assert parent != null;
        if (parent.hasFeature(ModelList.class)
                && parent.getFeature(ModelList.class).contains(node)) {
            return true;
        }
        return false;
    }

}
