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
package com.vaadin.server.communication.rpc;

import static org.junit.Assert.assertNull;

import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.nodefeature.AttachTemplateChildFeature;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

public class AttachTemplateChildRpcHandlerTest {

    @Test(expected = IllegalStateException.class)
    public void handleNode_error() {
        AttachTemplateChildRpcHandler handler = new AttachTemplateChildRpcHandler();

        int requestedId = 1;
        JsonObject object = Json.createObject();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, -1);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_ID, "id");

        StateNode node = Mockito.mock(StateNode.class);
        StateNode requested = Mockito.mock(StateNode.class);
        StateNode parentNode = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);
        Mockito.when(node.getOwner()).thenReturn(tree);
        Mockito.when(tree.getNodeById(requestedId)).thenReturn(requested);

        Element parentElement = Mockito.mock(Element.class);
        ShadowRoot shadowRoot = Mockito.mock(ShadowRoot.class);

        AttachTemplateChildFeature feature = new AttachTemplateChildFeature(
                node);
        feature.register(parentElement, requested);

        Mockito.when(parentElement.getShadowRoot())
                .thenReturn(Optional.of(shadowRoot));
        Mockito.when(node.getFeature(AttachTemplateChildFeature.class))
                .thenReturn(feature);
        Mockito.when(parentElement.getNode()).thenReturn(parentNode);
        Mockito.when(parentNode.getId()).thenReturn(3);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
    }

    @Test
    public void handleNode_requestedIdEqualsAssignedId() {
        AttachTemplateChildRpcHandler handler = new AttachTemplateChildRpcHandler();

        int requestedId = 1;
        JsonObject object = Json.createObject();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_ID, "id");

        StateNode node = Mockito.mock(StateNode.class);
        StateNode requested = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);

        Mockito.when(node.getOwner()).thenReturn(tree);

        Mockito.when(tree.getNodeById(requestedId)).thenReturn(requested);

        Mockito.when(requested.hasFeature(Mockito.any())).thenReturn(true);

        AttachTemplateChildFeature feature = new AttachTemplateChildFeature(
                node);
        Element parentNode = Mockito.mock(Element.class);
        ShadowRoot shadowRoot = Mockito.mock(ShadowRoot.class);
        Mockito.when(parentNode.getShadowRoot())
                .thenReturn(Optional.of(shadowRoot));

        feature.register(parentNode, requested);
        Mockito.when(node.getFeature(AttachTemplateChildFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
        Mockito.verify(shadowRoot).insertVirtualChild(Element.get(requested));
    }

    @Test
    public void handleNode_requestedIdAndAssignedIdAreDifferent() {
        AttachTemplateChildRpcHandler handler = new AttachTemplateChildRpcHandler();

        int requestedId = 1;
        int assignedId = 2;
        int index = 3;
        JsonObject object = Json.createObject();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, assignedId);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_ID, "id");

        StateNode node = Mockito.mock(StateNode.class);
        StateNode requested = Mockito.mock(StateNode.class);
        StateNode assigned = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);

        Mockito.when(node.getOwner()).thenReturn(tree);
        Mockito.when(tree.getNodeById(requestedId)).thenReturn(requested);
        Mockito.when(tree.getNodeById(assignedId)).thenReturn(assigned);

        Mockito.when(assigned.hasFeature(Mockito.any())).thenReturn(true);

        AttachTemplateChildFeature feature = new AttachTemplateChildFeature(
                node);
        Element parentNode = Mockito.mock(Element.class);
        ShadowRoot shadowRoot = Mockito.mock(ShadowRoot.class);
        Mockito.when(parentNode.getShadowRoot())
                .thenReturn(Optional.of(shadowRoot));

        feature.register(parentNode, requested);
        Mockito.when(node.getFeature(AttachTemplateChildFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
        Mockito.verify(shadowRoot, Mockito.times(0))
                .insertVirtualChild(Mockito.any());
    }

    private void assertNodeIsUnregistered(StateNode node, StateNode requested,
            AttachTemplateChildFeature feature) {
        Mockito.verify(requested).setParent(null);
        assertNull(feature.getParent(requested));
    }

}
