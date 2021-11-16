/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.HashMap;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.server.communication.rpc.AttachTemplateChildRpcHandler;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class AttachTemplateChildRpcHandlerTest {

    @Test(expected = IllegalStateException.class)
    public void handleNode_attachById_elementNotFound() {
        doHandleNode_attach_elementNotFound(Json.create("id"));
    }

    @Test(expected = IllegalStateException.class)
    public void handleNode_attachCustomElement_elementNotFound() {
        doHandleNode_attach_elementNotFound(Json.createNull());
    }

    @Test(expected = IllegalStateException.class)
    public void handleNode_attachByIdExistingRequest_throwReservedId() {
        doHandleNode_attach_throwReservedId(Json.create(2));
    }

    @Test(expected = IllegalStateException.class)
    public void handleNode_attachCustonElementCustomId_throwReservedId() {
        doHandleNode_attach_throwReservedId(Json.createNull());
    }

    @Test(expected = IllegalArgumentException.class)
    public void handleNode_success_throwIllegalInvocation() {
        assertHandleNode(1, Json.create("id"));
    }

    private void doHandleNode_attach_elementNotFound(JsonValue id) {
        assertHandleNode(-1, id);
    }

    private void doHandleNode_attach_throwReservedId(JsonValue id) {
        assertHandleNode(2, id);
    }

    private void assertHandleNode(int assignedId, JsonValue id) {
        AttachTemplateChildRpcHandler handler = new AttachTemplateChildRpcHandler();

        int requestedId = 1;
        JsonObject object = Json.createObject();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, assignedId);
        object.put(JsonConstants.RPC_ATTACH_ID, id);

        StateNode node = Mockito.mock(StateNode.class);
        StateNode parentNode = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);
        Mockito.when(node.getOwner()).thenReturn(tree);
        Mockito.when(node.getParent()).thenReturn(parentNode);
        Mockito.when(tree.getNodeById(requestedId)).thenReturn(node);
        Mockito.when(node.getChangeTracker(Mockito.any(), Mockito.any()))
                .thenReturn(new HashMap<>());

        ElementData data = new ElementData(node);
        data.setTag("foo");
        Mockito.when(node.getFeature(ElementData.class)).thenReturn(data);

        Mockito.when(parentNode.getId()).thenReturn(3);

        handler.handleNode(node, object);
    }

}
