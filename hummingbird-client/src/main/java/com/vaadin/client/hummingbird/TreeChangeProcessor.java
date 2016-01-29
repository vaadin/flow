/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import com.vaadin.client.WidgetUtil;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Updates a state tree based on changes in JSON format
 *
 * @since
 * @author Vaadin Ltd
 */
public class TreeChangeProcessor {
    private TreeChangeProcessor() {
        // Only static helpers here
    }

    /**
     * Update a state tree based on an JSON array of changes.
     *
     * @param tree
     *            the tree to update
     * @param changes
     *            the JSON array of changes
     */
    public static void processChanges(StateTree tree, JsonArray changes) {
        int length = changes.length();

        // Attach all nodes before doing anything else
        for (int i = 0; i < length; i++) {
            JsonObject change = changes.getObject(i);
            if (isAttach(change)) {
                int nodeId = (int) change.getNumber("node");

                StateNode node = new StateNode(nodeId, tree);
                tree.registerNode(node);
            }
        }

        // Then process all non-attach changes
        for (int i = 0; i < length; i++) {
            JsonObject change = changes.getObject(i);
            if (!isAttach(change)) {
                processChange(tree, change);
            }
        }
    }

    private static boolean isAttach(JsonObject change) {
        return "attach".equals(change.getString("type"));
    }

    /**
     * Update a state tree based on a JSON change. This method is public for
     * testing purposes.
     *
     * @param tree
     *            the tree to update
     * @param change
     *            the JSON change
     */
    public static void processChange(StateTree tree, JsonObject change) {
        String type = change.getString("type");
        int nodeId = (int) change.getNumber("node");

        StateNode node = tree.getNode(nodeId);
        assert node != null;

        switch (type) {
        case "splice":
            processSpliceChange(change, node);
            break;
        case "put":
            processPutChange(change, node);
            break;
        default:
            assert false : "Unsupported change type: " + type;
        }
    }

    private static void processPutChange(JsonObject change, StateNode node) {
        int nsId = (int) change.getNumber("ns");
        MapNamespace namespace = node.getMapNamespace(nsId);
        String key = change.getString("key");

        MapProperty property = namespace.getProperty(key);

        if (change.hasKey("value")) {
            JsonValue jsonValue = change.get("value");
            Object value = WidgetUtil.jsonValueToJavaValue(jsonValue);
            property.setValue(value);
        } else if (change.hasKey("nodeValue")) {
            int childId = (int) change.getNumber("nodeValue");
            StateNode child = node.getTree().getNode(childId);
            assert child != null;

            property.setValue(child);
        } else {
            assert false : "Change should have either value or nodeValue property: "
                    + change.toJson();
        }
    }

    private static void processSpliceChange(JsonObject change, StateNode node) {
        int nsId = (int) change.getNumber("ns");

        ListNamespace namespace = node.getListNamespace(nsId);

        int index = (int) change.getNumber("index");
        int remove;
        if (change.hasKey("remove")) {
            remove = (int) change.getNumber("remove");
        } else {
            remove = 0;
        }

        if (change.hasKey("add")) {
            JsonArray addJson = change.getArray("add");

            Object[] add = WidgetUtil.jsonArrayToJavaArray(addJson);

            namespace.splice(index, remove, add);
        } else if (change.hasKey("addNodes")) {
            JsonArray addNodes = change.getArray("addNodes");
            int length = addNodes.length();

            StateNode[] add = new StateNode[length];

            StateTree tree = node.getTree();
            for (int i = 0; i < length; i++) {
                int childId = (int) addNodes.getNumber(i);
                StateNode child = tree.getNode(childId);
                assert child != null : "No child node found with id " + childId;

                add[i] = child;
            }

            namespace.splice(index, remove, add);
        } else {
            namespace.splice(index, remove);
        }
    }
}
