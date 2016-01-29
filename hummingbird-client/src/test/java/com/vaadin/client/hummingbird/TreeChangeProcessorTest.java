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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.util.JsonStream;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class TreeChangeProcessorTest {
    private StateTree tree = new StateTree();
    private int rootId = tree.getRootNode().getId();
    private int ns = 0;

    @Test
    public void testPutChange() {
        JsonObject change = putChange(rootId, ns, "key", Json.create("value"));

        TreeChangeProcessor.processChange(tree, change);

        Object value = tree.getRootNode().getMapNamespace(ns).getProperty("key")
                .getValue();

        Assert.assertEquals("value", value);
    }

    @Test
    public void testPutNodeChange() {
        StateNode child = new StateNode(2, tree);
        tree.registerNode(child);

        JsonObject change = putNodeChange(rootId, ns, "key", child.getId());

        TreeChangeProcessor.processChange(tree, change);

        Object value = tree.getRootNode().getMapNamespace(ns).getProperty("key")
                .getValue();

        Assert.assertSame(child, value);
    }

    @Test
    public void testPrimitiveSpliceChange() {
        JsonObject change = spliceChange(rootId, ns, 0, 0, Json.create("foo"),
                Json.create("bar"));

        TreeChangeProcessor.processChange(tree, change);

        ListNamespace list = tree.getRootNode().getListNamespace(ns);

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

        ListNamespace list = tree.getRootNode().getListNamespace(ns);

        Assert.assertEquals(1, list.length());
        Assert.assertSame(child, list.get(0));
    }

    @Test
    public void testAttachNodeBeforePut() {
        int nodeId = 2;
        JsonArray changes = toArray(
                putChange(nodeId, ns, "key", Json.create("value")),
                attachChange(nodeId));

        TreeChangeProcessor.processChanges(tree, changes);

        // Basically ok if we get this far without exception, but verifying
        // value as well just to be on the safe side

        Object value = tree.getNode(nodeId).getMapNamespace(ns)
                .getProperty("key").getValue();
        Assert.assertEquals("value", value);
    }

    private static JsonArray toArray(JsonValue... changes) {
        return Arrays.stream(changes).collect(JsonStream.asArray());
    }

    private static JsonObject baseChange(int node, String type) {
        JsonObject json = Json.createObject();

        json.put("type", type);
        json.put("node", node);
        return json;
    }

    private static JsonObject mapBaseChange(int node, int ns, String key) {
        JsonObject json = baseChange(node, "put");
        json.put("ns", ns);
        json.put("key", key);
        return json;
    }

    private static JsonObject attachChange(int node) {
        return baseChange(node, "attach");
    }

    private static JsonObject putChange(int node, int ns, String key,
            JsonValue value) {
        JsonObject json = mapBaseChange(node, ns, key);
        json.put("value", value);

        return json;
    }

    private static JsonObject putNodeChange(int node, int ns, String key,
            int child) {
        JsonObject json = mapBaseChange(node, ns, key);

        json.put("nodeValue", child);

        return json;
    }

    private static JsonObject spliceBaseChange(int node, int ns, int index,
            int remove) {
        JsonObject json = baseChange(node, "splice");

        json.put("ns", ns);
        json.put("index", index);
        if (remove > 0) {
            json.put("remove", remove);
        }
        return json;
    }

    private static JsonObject spliceChange(int node, int ns, int index,
            int remove, JsonValue... add) {
        JsonObject json = spliceBaseChange(node, ns, index, remove);

        if (add != null && add.length != 0) {
            json.put("add", toArray(add));
        }

        return json;
    }

    private static JsonObject nodeSpliceChange(int node, int ns, int index,
            int remove, int... children) {
        JsonObject json = spliceBaseChange(node, ns, index, remove);

        if (children != null && children.length != 0) {
            JsonArray add = Arrays.stream(children).mapToObj(Json::create)
                    .collect(JsonStream.asArray());
            json.put("addNodes", add);
        }

        return json;
    }

}
