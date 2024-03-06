/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class GwtTreeChangeProcessorTest extends ClientEngineTestBase {
    public void testPrimitiveSplice() {
        StateTree tree = new StateTree(null);
        StateNode root = tree.getRootNode();
        int nsId = 1;

        JsonObject change = Json.createObject();
        change.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);
        change.put(JsonConstants.CHANGE_NODE, root.getId());
        change.put(JsonConstants.CHANGE_FEATURE, nsId);
        change.put(JsonConstants.CHANGE_SPLICE_INDEX, 0);
        JsonArray add = Json.createArray();
        add.set(0, "value");
        change.put(JsonConstants.CHANGE_SPLICE_ADD, add);

        TreeChangeProcessor.processChange(tree, change);

        NodeList list = root.getList(nsId);

        assertEquals(1, list.length());
        assertEquals("value", list.get(0));
    }
}
