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
package com.vaadin.flow.server.communication.rpc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.internal.nodefeature.SynchronizedPropertiesList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Model map synchronization RPC handler.
 *
 * @see JsonConstants#RPC_TYPE_MAP_SYNC
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class MapSyncRpcHandler extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_MAP_SYNC;
    }

    @Override
    protected Optional<Runnable> handleNode(StateNode node,
            JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_FEATURE);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY_VALUE);

        int featureId = (int) invocationJson
                .getNumber(JsonConstants.RPC_FEATURE);
        Class<? extends NodeFeature> feature = NodeFeatureRegistry
                .getFeature(featureId);
        assert NodeMap.class.isAssignableFrom(feature);
        assert ElementPropertyMap.class.equals(feature);

        boolean isEnabled = node.isEnabled();

        List<DisabledUpdateMode> seenUpdateModes = new ArrayList<>();

        String property = invocationJson.getString(JsonConstants.RPC_PROPERTY);

        if (node.hasFeature(SynchronizedPropertiesList.class)) {
            DisabledUpdateMode syncMode = node
                    .getFeature(SynchronizedPropertiesList.class)
                    .getDisabledUpdateMode(property);

            if (syncMode != null) {
                seenUpdateModes.add(syncMode);
            }
        }
        if (node.hasFeature(ElementListenerMap.class)) {
            DisabledUpdateMode eventMode = node
                    .getFeature(ElementListenerMap.class)
                    .getPropertySynchronizationMode(property);

            if (eventMode != null) {
                seenUpdateModes.add(eventMode);
            }
        }

        DisabledUpdateMode updateMode = seenUpdateModes.stream()
                .reduce(DisabledUpdateMode::mostPermissive).orElse(null);

        if (isEnabled) {
            return enqueuePropertyUpdate(node, invocationJson, feature,
                    property);
        } else if (DisabledUpdateMode.ALWAYS.equals(updateMode)) {
            LoggerFactory.getLogger(MapSyncRpcHandler.class)
                    .trace("Property update request for disabled element is received from the client side. "
                            + "Change will be applied since the property '{}' always allows its update.",
                            property);
            return enqueuePropertyUpdate(node, invocationJson, feature,
                    property);
        } else {
            LoggerFactory.getLogger(MapSyncRpcHandler.class)
                    .warn("Property update request for disabled element is received from the client side. "
                            + "The property is '{}'. Request is ignored.",
                            property);
        }
        return Optional.empty();
    }

    private Optional<Runnable> enqueuePropertyUpdate(StateNode node,
            JsonObject invocationJson, Class<? extends NodeFeature> feature,
            String property) {
        Serializable value = JsonCodec.decodeWithoutTypeInfo(
                invocationJson.get(JsonConstants.RPC_PROPERTY_VALUE));

        value = tryConvert(value, node);

        return Optional.of(node.getFeature(ElementPropertyMap.class)
                .deferredUpdateFromClient(property, value));
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
