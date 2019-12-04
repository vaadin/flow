/*
 * Copyright 2000-2019 Vaadin Ltd.
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
