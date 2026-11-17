/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.AttachExistingElementFeature;
import com.vaadin.flow.shared.JsonConstants;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachExistingElementRpcHandlerTest {

    @Test
    void handleNode_error() {
        AttachExistingElementRpcHandler handler = new AttachExistingElementRpcHandler();

        int requestedId = 1;
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, -1);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_INDEX, -1);

        StateNode node = mock(StateNode.class);
        StateNode requested = mock(StateNode.class);
        StateTree tree = mock(StateTree.class);
        when(node.getOwner()).thenReturn(tree);
        when(tree.getNodeById(requestedId)).thenReturn(requested);

        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);
        Node<?> parentNode = mock(Node.class);
        ChildElementConsumer consumer = mock(ChildElementConsumer.class);
        Element sibling = mock(Element.class);
        feature.register(parentNode, sibling, requested, consumer);
        when(node.getFeature(AttachExistingElementFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        verify(consumer).onError(parentNode, "div", sibling);
        assertNodeIsUnregistered(node, requested, feature);
    }

    @Test
    void handleNode_requestedIdEqualsAssignedId() {
        AttachExistingElementRpcHandler handler = new AttachExistingElementRpcHandler();

        int requestedId = 1;
        int index = 2;
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_INDEX, index);

        StateNode node = mock(StateNode.class);
        StateNode requested = mock(StateNode.class);
        StateTree tree = mock(StateTree.class);

        when(node.getOwner()).thenReturn(tree);
        when(tree.getNodeById(requestedId)).thenReturn(requested);

        when(requested.hasFeature(any())).thenReturn(true);

        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);
        Node<?> parentNode = mock(Node.class);
        ChildElementConsumer consumer = mock(ChildElementConsumer.class);
        Element sibling = mock(Element.class);
        feature.register(parentNode, sibling, requested, consumer);
        when(node.getFeature(AttachExistingElementFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
        verify(parentNode).insertChild(index, Element.get(requested));
        verify(consumer).accept(Element.get(requested));
    }

    @Test
    void handleNode_requestedIdAndAssignedIdAreDifferent() {
        AttachExistingElementRpcHandler handler = new AttachExistingElementRpcHandler();

        int requestedId = 1;
        int assignedId = 2;
        int index = 3;
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        object.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, assignedId);
        object.put(JsonConstants.RPC_ATTACH_TAG_NAME, "div");
        object.put(JsonConstants.RPC_ATTACH_INDEX, index);

        StateNode node = mock(StateNode.class);
        StateNode requested = mock(StateNode.class);
        StateNode assigned = mock(StateNode.class);
        StateTree tree = mock(StateTree.class);

        when(node.getOwner()).thenReturn(tree);
        when(tree.getNodeById(requestedId)).thenReturn(requested);
        when(tree.getNodeById(assignedId)).thenReturn(assigned);

        when(assigned.hasFeature(any())).thenReturn(true);

        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);
        Node<?> parentNode = mock(Node.class);
        ChildElementConsumer consumer = mock(ChildElementConsumer.class);
        Element sibling = mock(Element.class);
        feature.register(parentNode, sibling, requested, consumer);
        when(node.getFeature(AttachExistingElementFeature.class))
                .thenReturn(feature);

        handler.handleNode(node, object);

        assertNodeIsUnregistered(node, requested, feature);
        verify(parentNode, times(0)).insertChild(index, Element.get(assigned));
        verify(consumer).accept(Element.get(assigned));
    }

    private void assertNodeIsUnregistered(StateNode node, StateNode requested,
            AttachExistingElementFeature feature) {
        verify(requested).setParent(null);
        assertNull(feature.getParent(requested));
        assertNull(feature.getCallback(requested));
        assertNull(feature.getPreviousSibling(node));
    }

}
