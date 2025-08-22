/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.internal.nodefeature.PropertyChangeDeniedException;
import com.vaadin.flow.shared.JsonConstants;


/**
 * Model map synchronization RPC handler.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
            JsonNode invocationJson) {
        assert invocationJson.has(JsonConstants.RPC_FEATURE);
        assert invocationJson.has(JsonConstants.RPC_PROPERTY);
        assert invocationJson.has(JsonConstants.RPC_PROPERTY_VALUE);

        int featureId = invocationJson.get(JsonConstants.RPC_FEATURE)
                .intValue();
        Class<? extends NodeFeature> feature = NodeFeatureRegistry
                .getFeature(featureId);
        assert NodeMap.class.isAssignableFrom(feature);
        assert ElementPropertyMap.class.equals(feature);

        boolean isEnabled = node.isEnabled();

        List<DisabledUpdateMode> seenUpdateModes = new ArrayList<>();

        String property = invocationJson.get(JsonConstants.RPC_PROPERTY)
                .asText();

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
            return enqueuePropertyUpdate(node, invocationJson, property);
        } else if (DisabledUpdateMode.ALWAYS.equals(updateMode)) {
            LoggerFactory.getLogger(MapSyncRpcHandler.class).trace(
                    "Property update request for disabled element is received from the client side. "
                            + "Change will be applied since the property '{}' always allows its update.",
                    property);
            return enqueuePropertyUpdate(node, invocationJson, property);
        } else {
            final Logger logger = LoggerFactory
                    .getLogger(MapSyncRpcHandler.class);
            Optional<Serializable> featureProperty = node
                    .getFeatureIfInitialized(ElementPropertyMap.class)
                    .map(feat -> feat.getProperty(property));
            if (featureProperty.isPresent()) {
                logger.warn(
                        "Property update request for disabled element is received from the client side. "
                                + "The property is '{}'. Request is ignored.",
                        property);
            } else {
                logger.debug(
                        "Ignored property '{}' change for disabled element. Most likely client sent the "
                                + "default value as no value has been set for the property.",
                        property);
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean allowInert(UI ui, JsonNode invocationJson) {
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));
        if (node != null && node.hasFeature(ElementListenerMap.class)) {
            ElementListenerMap listenerMap = node
                    .getFeature(ElementListenerMap.class);
            return invocationJson.has(JsonConstants.RPC_PROPERTY)
                    && listenerMap.hasAllowInertForProperty(invocationJson
                            .get(JsonConstants.RPC_PROPERTY).asText());
        } else {
            return super.allowInert(ui, invocationJson);
        }
    }

    private Optional<Runnable> enqueuePropertyUpdate(StateNode node,
            JsonNode invocationJson, String property) {
        Serializable value = JacksonCodec
                .decodeWithoutTypeInfo((BaseJsonNode) invocationJson
                        .get(JsonConstants.RPC_PROPERTY_VALUE));

        value = tryConvert(value, node);

        try {
            return Optional.of(node.getFeature(ElementPropertyMap.class)
                    .deferredUpdateFromClient(property, value));
        } catch (PropertyChangeDeniedException exception) {
            throw new IllegalArgumentException(
                    getVetoPropertyUpdateMessage(node, property), exception);
        }
    }

    private boolean hasElement(StateNode node) {
        return node != null && node.hasFeature(ElementData.class);
    }

    private String getVetoPropertyUpdateMessage(StateNode node,
            String property) {
        if (hasElement(node)) {
            Element element = Element.get(node);
            String tag = element.getTag();
            Optional<Component> component = element.getComponent();
            String prefix;
            if (component.isPresent()) {
                prefix = "Component " + component.get().getClass().getName();
            } else {
                prefix = "Element with tag '" + tag + "'";
            }
            return String.format(
                    "%s tries to update (sub)property '%s' whose update is not allowed. "
                            + "For security reasons, the property must be defined as synchronized through the Element's API.",
                    prefix, property);
        } else if (node != null) {
            return getVetoPropertyUpdateMessage(node.getParent(), property);
        }
        return "";
    }

    private Serializable tryConvert(Serializable value, StateNode context) {
        if (value instanceof ObjectNode) {
            ObjectNode json = (ObjectNode) value;
            if (json.has("nodeId")) {
                StateTree tree = (StateTree) context.getOwner();
                double id = json.get("nodeId").doubleValue();
                StateNode stateNode = tree.getNodeById((int) id);
                return tryCopyStateNode(stateNode, json);
            }
        }
        return value;
    }

    private Serializable tryCopyStateNode(StateNode node,
            ObjectNode properties) {
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
