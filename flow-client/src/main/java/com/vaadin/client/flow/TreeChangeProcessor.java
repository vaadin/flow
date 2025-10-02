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
package com.vaadin.client.flow;

import com.google.gwt.core.client.GWT;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.util.ClientJsonCodec;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Updates a state tree based on changes in JSON format.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        assert !tree.isUpdateInProgress()
                : "Previous tree change processing has not completed";
        try {
            tree.setUpdateInProgress(true);

            // Attach all nodes before doing anything else
            JsSet<StateNode> nodes = processAttachChanges(tree, changes);

            // Then process all non-attach changes
            int length = changes.length();
            for (int i = 0; i < length; i++) {
                JsonObject change = changes.getObject(i);
                if (!isAttach(change)) {
                    final StateNode value = processChange(tree, change);
                    if (value != null) {
                        nodes.add(value);
                    }
                }
            }
            return nodes;
        } finally {
            tree.setUpdateInProgress(false);
            tree.setResync(false);
        }
    }

    private static JsSet<StateNode> processAttachChanges(StateTree tree,
            JsonArray changes) {
        JsSet<StateNode> nodes = JsCollections.set();
        int length = changes.length();
        for (int i = 0; i < length; i++) {
            JsonObject change = changes.getObject(i);
            if (isAttach(change)) {
                int nodeId = (int) change.getNumber(JsonConstants.CHANGE_NODE);

                if (nodeId != tree.getRootNode().getId()) {
                    StateNode node = new StateNode(nodeId, tree);
                    tree.registerNode(node);
                    nodes.add(node);
                }
            }
        }
        return nodes;
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
        if (node == null && tree.isResync()) {
            // Resync should not stop handling changes
            return node;
        }
        assert node != null : "No attached node found";

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
        assert change.hasKey(JsonConstants.CHANGE_FEATURE_TYPE)
                : "Change doesn't contain feature type. Don't know how to populate feature";
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
        } else if (change.hasKey(JsonConstants.CHANGE_PUT_NODE_VALUE)) {
            int childId = (int) change
                    .getNumber(JsonConstants.CHANGE_PUT_NODE_VALUE);
            StateNode child = node.getTree().getNode(childId);
            assert child != null;
            child.setParent(node);

            property.setValue(child);
        } else {
            assert false
                    : "Change should have either value or nodeValue property: "
                            + WidgetUtil.stringify(change);
        }
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

            JsArray<Object> add = jsonArrayAsJsArray(addJson);

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

    /**
     * Converts a JSON array to a JS array. This is a no-op in compiled
     * JavaScript, but needs special handling for tests running in the JVM.
     *
     * @param jsonArray
     *            the JSON array to convert
     * @return the converted JS array
     */
    private static JsArray<Object> jsonArrayAsJsArray(JsonArray jsonArray) {
        JsArray<Object> jsArray;
        if (GWT.isScript()) {
            jsArray = WidgetUtil.crazyJsCast(jsonArray);
        } else {
            jsArray = JsCollections.array();
            for (int i = 0; i < jsonArray.length(); i++) {
                // Decode the value to unwrap JsonString to primitive string
                Object decoded = ClientJsonCodec
                        .decodeWithoutTypeInfo(jsonArray.get(i));
                jsArray.push(decoded);
            }
        }
        return jsArray;
    }

}
