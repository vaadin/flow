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
package com.vaadin.client.flow;

import com.vaadin.client.PolymerUtils;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.ClientJsonCodec;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Updates a state tree based on changes in JSON format.
 *
 * @author Vaadin Ltd
 */
public class TreeChangeProcessor {
    private TreeChangeProcessor() {
        // Only static helpers here
    }

    /**
     * Update a state tree based on a JSON array of changes.
     *
     * @param tree
     *            the tree to update
     * @param changes
     *            the JSON array of changes
     * @return a set of updated nodes addressed by the {@code changes}
     */
    public static JsSet<StateNode> processChanges(StateTree tree,
            JsonArray changes) {
        assert !tree
                .isUpdateInProgress() : "Previous tree change processing has not completed";
        try {
            tree.setUpdateInProgress(true);
            int length = changes.length();

            JsSet<StateNode> nodes = JsCollections.set();

            // Attach all nodes before doing anything else
            for (int i = 0; i < length; i++) {
                JsonObject change = changes.getObject(i);
                if (isAttach(change)) {
                    int nodeId = (int) change
                            .getNumber(JsonConstants.CHANGE_NODE);

                    StateNode node = new StateNode(nodeId, tree);
                    tree.registerNode(node);
                    nodes.add(node);
                }
            }

            // Then process all non-attach changes
            for (int i = 0; i < length; i++) {
                JsonObject change = changes.getObject(i);
                if (!isAttach(change)) {
                    nodes.add(processChange(tree, change));
                }
            }
            return nodes;
        } finally {
            tree.setUpdateInProgress(false);
        }

    }

    private static boolean isAttach(JsonObject change) {
        return JsonConstants.CHANGE_TYPE_ATTACH
                .equals(change.getString(JsonConstants.CHANGE_TYPE));
    }

    /**
     * Update a state tree based on a JSON change. This method is public for
     * testing purposes.
     *
     * @param tree
     *            the tree to update
     * @param change
     *            the JSON change
     * @return the updated node addressed by the provided {@code change}
     */
    public static StateNode processChange(StateTree tree, JsonObject change) {
        String type = change.getString(JsonConstants.CHANGE_TYPE);
        int nodeId = (int) change.getNumber(JsonConstants.CHANGE_NODE);

        StateNode node = tree.getNode(nodeId);
        assert node != null;

        switch (type) {
        case JsonConstants.CHANGE_TYPE_NOOP:
            populateFeature(change, node);
            break;
        case JsonConstants.CHANGE_TYPE_SPLICE:
            processSpliceChange(change, node);
            break;
        case JsonConstants.CHANGE_TYPE_PUT:
            processPutChange(change, node);
            break;
        case JsonConstants.CHANGE_TYPE_REMOVE:
            processRemoveChange(change, node);
            break;
        case JsonConstants.CHANGE_TYPE_DETACH:
            processDetachChange(node);
            break;
        case JsonConstants.CHANGE_TYPE_CLEAR:
            processClearChange(change, node);
            break;
        default:
            assert false : "Unsupported change type: " + type;
        }
        return node;
    }

    private static void processDetachChange(StateNode node) {
        node.getTree().unregisterNode(node);
        node.setParent(null);
    }

    private static void populateFeature(JsonObject change, StateNode node) {
        assert change.hasKey(
                JsonConstants.CHANGE_FEATURE_TYPE) : "Change doesn't contain feature type. Don't know how to populate feature";
        int featureId = (int) change.getNumber(JsonConstants.CHANGE_FEATURE);
        if (change.getBoolean(JsonConstants.CHANGE_FEATURE_TYPE)) {
            // list feature
            node.getList(featureId);
        } else {
            node.getMap(featureId);
        }
    }

    private static void processPutChange(JsonObject change, StateNode node) {
        MapProperty property = findProperty(change, node);

        if (change.hasKey(JsonConstants.CHANGE_PUT_VALUE)) {
            JsonValue jsonValue = change.get(JsonConstants.CHANGE_PUT_VALUE);
            Object value = ClientJsonCodec.decodeWithoutTypeInfo(jsonValue);
            property.setValue(value);
            Reactive.addPostFlushListener(() -> setValueInsideListIfApplicable(
                    node, property, value));
        } else if (change.hasKey(JsonConstants.CHANGE_PUT_NODE_VALUE)) {
            int childId = (int) change
                    .getNumber(JsonConstants.CHANGE_PUT_NODE_VALUE);
            StateNode child = node.getTree().getNode(childId);
            assert child != null;
            child.setParent(node);

            property.setValue(child);
        } else {
            assert false : "Change should have either value or nodeValue property: "
                    + WidgetUtil.stringify(change);
        }
    }

    /**
     * Sets a property value on objects inside lists defined in a TemplateModel.
     * It's a NO-OP in case the property is not part of an object inside a list,
     * or if the list is not a direct property of the template model.
     * 
     * @param node
     *            the property node
     * @param property
     *            the property to be updated
     * @param value
     *            the new value of the property
     */
    private static void setValueInsideListIfApplicable(StateNode node,
            MapProperty property, Object value) {

        StateNode parent = getParentNodeAttachedToADomNode(node);
        if (parent == null
                || !parent.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
            return;
        }

        NodeMap feature = parent.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        feature.forEachProperty((parentProperty,
                parentPropertyName) -> updatePropertyInsideList(parent, node,
                        parentProperty, parentPropertyName, property, value));
    }

    /**
     * Gets the first parent node that also has a DOM Node attached to it.
     * 
     * @param node
     *            the node
     * @return the first parent node with a DOM Node, or <code>null</code> if
     *         none can be found
     */
    private static StateNode getParentNodeAttachedToADomNode(StateNode node) {
        StateNode parent = node.getParent();
        while (parent != null && parent.getDomNode() == null) {
            parent = parent.getParent();
        }
        return parent;
    }

    private static void updatePropertyInsideList(StateNode parent,
            StateNode node, MapProperty parentProperty,
            String parentPropertyName, MapProperty nodeProperty, Object value) {

        if (!(parentProperty.getValue() instanceof StateNode)) {
            return;
        }

        StateNode mapValue = (StateNode) parentProperty.getValue();
        if (!mapValue.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
            return;
        }

        int indexInTheList = -1;
        NodeList children = mapValue.getList(NodeFeatures.TEMPLATE_MODELLIST);

        for (int i = 0; i < children.length(); i++) {
            Object object = children.get(i);
            if (node.equals(object)) {
                indexInTheList = i;
                break;
            }
        }

        if (indexInTheList < 0) {
            return;
        }

        StringBuilder path = new StringBuilder(parentPropertyName).append(".")
                .append(indexInTheList);

        if (!node.hasFeature(NodeFeatures.BASIC_TYPE_VALUE)) {
            path.append(".").append(nodeProperty.getName());
        }

        PolymerUtils.setPropertyValue(parent.getDomNode(), path.toString(),
                PolymerUtils.convertToJson(value));
    }

    private static void processRemoveChange(JsonObject change, StateNode node) {
        MapProperty property = findProperty(change, node);

        property.removeValue();
    }

    private static MapProperty findProperty(JsonObject change, StateNode node) {
        int nsId = (int) change.getNumber(JsonConstants.CHANGE_FEATURE);
        NodeMap map = node.getMap(nsId);
        String key = change.getString(JsonConstants.CHANGE_MAP_KEY);

        return map.getProperty(key);
    }

    private static void processSpliceChange(JsonObject change, StateNode node) {
        int nsId = (int) change.getNumber(JsonConstants.CHANGE_FEATURE);

        NodeList list = node.getList(nsId);

        int index = (int) change.getNumber(JsonConstants.CHANGE_SPLICE_INDEX);
        int remove;
        if (change.hasKey(JsonConstants.CHANGE_SPLICE_REMOVE)) {
            remove = (int) change.getNumber(JsonConstants.CHANGE_SPLICE_REMOVE);
        } else {
            remove = 0;
        }

        if (change.hasKey(JsonConstants.CHANGE_SPLICE_ADD)) {
            JsonArray addJson = change
                    .getArray(JsonConstants.CHANGE_SPLICE_ADD);

            JsArray<Object> add = ClientJsonCodec.jsonArrayAsJsArray(addJson);

            list.splice(index, remove, add);
        } else if (change.hasKey(JsonConstants.CHANGE_SPLICE_ADD_NODES)) {
            JsonArray addNodes = change
                    .getArray(JsonConstants.CHANGE_SPLICE_ADD_NODES);
            int length = addNodes.length();

            JsArray<StateNode> add = JsCollections.array();

            StateTree tree = node.getTree();
            for (int i = 0; i < length; i++) {
                int childId = (int) addNodes.getNumber(i);
                StateNode child = tree.getNode(childId);
                assert child != null : "No child node found with id " + childId;
                child.setParent(node);

                add.set(i, child);
            }

            list.splice(index, remove, add);
        } else {
            list.splice(index, remove);
        }
    }

    private static void processClearChange(JsonObject change, StateNode node) {
        int nsId = (int) change.getNumber(JsonConstants.CHANGE_FEATURE);
        NodeList list = node.getList(nsId);
        list.clear();
    }
}
