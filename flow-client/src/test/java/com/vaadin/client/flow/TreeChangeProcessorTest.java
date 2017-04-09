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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class TreeChangeProcessorTest {
    private StateTree tree = new StateTree(null);
    private int rootId = tree.getRootNode().getId();
    private int ns = 0;

    private String myKey = "myKey";
    private String myValue = "myValue";

    @Test
    public void testPutChange() {
        JsonObject change = putChange(rootId, ns, myKey, Json.create(myValue));

        TreeChangeProcessor.processChange(tree, change);

        Object value = tree.getRootNode().getMap(ns).getProperty(myKey)
                .getValue();

        Assert.assertEquals(myValue, value);
    }

    @Test
    public void testMapRemoveChange() {
        MapProperty property = tree.getRootNode().getMap(ns).getProperty(myKey);
        property.setValue(myValue);

        JsonObject change = removeChange(rootId, ns, myKey);

        TreeChangeProcessor.processChange(tree, change);

        Assert.assertFalse(property.hasValue());
    }

    @Test
    public void testMapReAdd() {
        MapProperty property = tree.getRootNode().getMap(ns).getProperty(myKey);
        property.setValue(myValue);

        JsonObject change = removeChange(rootId, ns, myKey);

        TreeChangeProcessor.processChange(tree, change);

        StateNode child = new StateNode(2, tree);
        tree.registerNode(child);

        change = putNodeChange(rootId, ns, myKey, child.getId());

        TreeChangeProcessor.processChange(tree, change);

        Assert.assertSame(child, property.getValue());
    }

    @Test
    public void testPutNodeChange() {
        StateNode child = new StateNode(2, tree);
        tree.registerNode(child);

        JsonObject change = putNodeChange(rootId, ns, myKey, child.getId());

        TreeChangeProcessor.processChange(tree, change);

        Object value = tree.getRootNode().getMap(ns).getProperty(myKey)
                .getValue();

        Assert.assertSame(child, value);
    }

    @Test
    public void testPrimitiveSpliceChange() {
        JsonObject change = spliceChange(rootId, ns, 0, 0, Json.create("foo"),
                Json.create("bar"));

        TreeChangeProcessor.processChange(tree, change);

        NodeList list = tree.getRootNode().getList(ns);

        Assert.assertEquals(2, list.length());
        Assert.assertEquals("foo", list.get(0));
        Assert.assertEquals("bar", list.get(1));

        change = spliceChange(rootId, ns, 1, 0, Json.create("baz"));

        TreeChangeProcessor.processChange(tree, change);

        Assert.assertEquals(3, list.length());
        Assert.assertEquals("foo", list.get(0));
        Assert.assertEquals("baz", list.get(1));
        Assert.assertEquals("bar", list.get(2));

        change = spliceChange(rootId, ns, 1, 1);

        TreeChangeProcessor.processChange(tree, change);

        Assert.assertEquals(2, list.length());
        Assert.assertEquals("foo", list.get(0));
        Assert.assertEquals("bar", list.get(1));
    }

    @Test
    public void testNodeSpliceChange() {
        StateNode child = new StateNode(2, tree);
        tree.registerNode(child);

        JsonObject change = nodeSpliceChange(rootId, ns, 0, 0, child.getId());

        TreeChangeProcessor.processChange(tree, change);

        NodeList list = tree.getRootNode().getList(ns);

        Assert.assertEquals(1, list.length());
        Assert.assertSame(child, list.get(0));
    }

    @Test
    public void testAttachNodeBeforePut() {
        int nodeId = 2;
        JsonArray changes = toArray(
                putChange(nodeId, ns, myKey, Json.create(myValue)),
                attachChange(nodeId));

        TreeChangeProcessor.processChanges(tree, changes);

        // Basically ok if we get this far without exception, but verifying
        // value as well just to be on the safe side

        Object value = tree.getNode(nodeId).getMap(ns).getProperty(myKey)
                .getValue();
        Assert.assertEquals(myValue, value);
    }

    @Test
    public void testDetachRemovesNode() {
        AtomicInteger unregisterCount = new AtomicInteger(0);

        StateNode childNode = new StateNode(2, tree);
        childNode.addUnregisterListener(e -> unregisterCount.incrementAndGet());

        tree.registerNode(childNode);

        Assert.assertSame(childNode, tree.getNode(childNode.getId()));
        Assert.assertEquals(0, unregisterCount.get());

        JsonArray changes = toArray(detachChange(childNode.getId()));
        TreeChangeProcessor.processChanges(tree, changes);

        Assert.assertNull(tree.getNode(childNode.getId()));
        Assert.assertEquals(1, unregisterCount.get());
    }

    private static JsonArray toArray(JsonValue... changes) {
        return Arrays.stream(changes).collect(JsonUtils.asArray());
    }

    private static JsonObject baseChange(int node, String type) {
        JsonObject json = Json.createObject();

        json.put(JsonConstants.CHANGE_TYPE, type);
        json.put(JsonConstants.CHANGE_NODE, node);
        return json;
    }

    private static JsonObject mapBaseChange(int node, int ns, String type,
            String key) {
        JsonObject json = baseChange(node, type);
        json.put(JsonConstants.CHANGE_FEATURE, ns);
        json.put(JsonConstants.CHANGE_MAP_KEY, key);
        return json;
    }

    private static JsonObject attachChange(int node) {
        return baseChange(node, JsonConstants.CHANGE_TYPE_ATTACH);
    }

    private static JsonObject detachChange(int node) {
        return baseChange(node, JsonConstants.CHANGE_TYPE_DETACH);
    }

    private static JsonObject putChange(int node, int ns, String key,
            JsonValue value) {
        JsonObject json = mapBaseChange(node, ns, JsonConstants.CHANGE_TYPE_PUT,
                key);
        json.put(JsonConstants.CHANGE_PUT_VALUE, value);

        return json;
    }

    private static JsonObject removeChange(int node, int ns, String key) {
        return mapBaseChange(node, ns, JsonConstants.CHANGE_TYPE_REMOVE, key);
    }

    private static JsonObject putNodeChange(int node, int ns, String key,
            int child) {
        JsonObject json = mapBaseChange(node, ns, JsonConstants.CHANGE_TYPE_PUT,
                key);

        json.put(JsonConstants.CHANGE_PUT_NODE_VALUE, child);

        return json;
    }

    private static JsonObject spliceBaseChange(int node, int ns, int index,
            int remove) {
        JsonObject json = baseChange(node, JsonConstants.CHANGE_TYPE_SPLICE);

        json.put(JsonConstants.CHANGE_FEATURE, ns);
        json.put(JsonConstants.CHANGE_SPLICE_INDEX, index);
        if (remove > 0) {
            json.put(JsonConstants.CHANGE_SPLICE_REMOVE, remove);
        }
        return json;
    }

    private static JsonObject spliceChange(int node, int ns, int index,
            int remove, JsonValue... add) {
        JsonObject json = spliceBaseChange(node, ns, index, remove);

        if (add != null && add.length != 0) {
            json.put(JsonConstants.CHANGE_SPLICE_ADD, toArray(add));
        }

        return json;
    }

    private static JsonObject nodeSpliceChange(int node, int ns, int index,
            int remove, int... children) {
        JsonObject json = spliceBaseChange(node, ns, index, remove);

        if (children != null && children.length != 0) {
            JsonArray add = Arrays.stream(children).mapToObj(Json::create)
                    .collect(JsonUtils.asArray());
            json.put(JsonConstants.CHANGE_SPLICE_ADD_NODES, add);
        }

        return json;
    }

}
