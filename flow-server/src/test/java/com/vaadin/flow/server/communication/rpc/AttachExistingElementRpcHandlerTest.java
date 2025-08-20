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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.AttachExistingElementFeature;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

public class AttachExistingElementRpcHandlerTest {

    @Test
    public void handleNode_error() {
        AttachExistingElementRpcHandler handler = new AttachExistingElementRpcHandler();

        int requestedId = 1;
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, -1);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_INDEX, -1);

        StateNode node = Mockito.mock(StateNode.class);
        StateNode requested = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);
        Mockito.when(node.getOwner()).thenReturn(tree);
        Mockito.when(tree.getNodeById(requestedId)).thenReturn(requested);

        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);
        Node<?> parentNode = Mockito.mock(Node.class);
        ChildElementConsumer consumer = Mockito
                .mock(ChildElementConsumer.class);
        Element sibling = Mockito.mock(Element.class);
        feature.register(parentNode, sibling, requested, consumer);
        Mockito.when(node.getFeature(AttachExistingElementFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        Mockito.verify(consumer).onError(parentNode, "div", sibling);
        assertNodeIsUnregistered(node, requested, feature);
    }

    @Test
    public void handleNode_requestedIdEqualsAssignedId() {
        AttachExistingElementRpcHandler handler = new AttachExistingElementRpcHandler();

        int requestedId = 1;
        int index = 2;
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_INDEX, index);

        StateNode node = Mockito.mock(StateNode.class);
        StateNode requested = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);

        Mockito.when(node.getOwner()).thenReturn(tree);
        Mockito.when(tree.getNodeById(requestedId)).thenReturn(requested);

        Mockito.when(requested.hasFeature(Mockito.any())).thenReturn(true);

        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);
        Node<?> parentNode = Mockito.mock(Node.class);
        ChildElementConsumer consumer = Mockito
                .mock(ChildElementConsumer.class);
        Element sibling = Mockito.mock(Element.class);
        feature.register(parentNode, sibling, requested, consumer);
        Mockito.when(node.getFeature(AttachExistingElementFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
        Mockito.verify(parentNode).insertChild(index, Element.get(requested));
        Mockito.verify(consumer).accept(Element.get(requested));
    }

    @Test
    public void handleNode_requestedIdAndAssignedIdAreDifferent() {
        AttachExistingElementRpcHandler handler = new AttachExistingElementRpcHandler();

        int requestedId = 1;
        int assignedId = 2;
        int index = 3;
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, assignedId);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_INDEX, index);

        StateNode node = Mockito.mock(StateNode.class);
        StateNode requested = Mockito.mock(StateNode.class);
        StateNode assigned = Mockito.mock(StateNode.class);
        StateTree tree = Mockito.mock(StateTree.class);

        Mockito.when(node.getOwner()).thenReturn(tree);
        Mockito.when(tree.getNodeById(requestedId)).thenReturn(requested);
        Mockito.when(tree.getNodeById(assignedId)).thenReturn(assigned);

        Mockito.when(assigned.hasFeature(Mockito.any())).thenReturn(true);

        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);
        Node<?> parentNode = Mockito.mock(Node.class);
        ChildElementConsumer consumer = Mockito
                .mock(ChildElementConsumer.class);
        Element sibling = Mockito.mock(Element.class);
        feature.register(parentNode, sibling, requested, consumer);
        Mockito.when(node.getFeature(AttachExistingElementFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
        Mockito.verify(parentNode, Mockito.times(0)).insertChild(index,
                Element.get(assigned));
        Mockito.verify(consumer).accept(Element.get(assigned));
    }

    private void assertNodeIsUnregistered(StateNode node, StateNode requested,
            AttachExistingElementFeature feature) {
        Mockito.verify(requested).setParent(null);
        Assert.assertNull(feature.getParent(requested));
        Assert.assertNull(feature.getCallback(requested));
        Assert.assertNull(feature.getPreviousSibling(node));
    }

}
