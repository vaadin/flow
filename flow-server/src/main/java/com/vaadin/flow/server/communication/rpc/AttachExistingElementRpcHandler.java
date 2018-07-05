/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.Optional;

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.AttachExistingElementFeature;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * RPC handler for a client-side response on attach existing element request.
 * 
 * @see JsonConstants#RPC_ATTACH_EXISTING_ELEMENT
 * @see JsonConstants#RPC_ATTACH_ASSIGNED_ID
 * @see JsonConstants#RPC_ATTACH_REQUESTED_ID
 * @see JsonConstants#RPC_ATTACH_INDEX
 * @see JsonConstants#RPC_ATTACH_TAG_NAME
 * @author Vaadin Ltd
 * @since 1.0
 *
 */

public class AttachExistingElementRpcHandler
        extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_ATTACH_EXISTING_ELEMENT;
    }

    @Override
    protected Optional<Runnable> handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_REQUESTED_ID);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_ASSIGNED_ID);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_TAG_NAME);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_INDEX);

        int requestedId = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_REQUESTED_ID);
        int assignedId = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_ASSIGNED_ID);
        String tag = invocationJson
                .getString(JsonConstants.RPC_ATTACH_TAG_NAME);
        int index = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_INDEX);

        AttachExistingElementFeature feature = node
                .getFeature(AttachExistingElementFeature.class);

        StateTree tree = (StateTree) node.getOwner();
        StateNode requestedNode = tree.getNodeById(requestedId);
        if (assignedId == -1) {
            // handle an error
            assert index == -1;

            ChildElementConsumer callback = feature.getCallback(requestedNode);
            assert callback != null;
            callback.onError(feature.getParent(requestedNode), tag,
                    feature.getPreviousSibling(requestedNode));

            feature.unregister(requestedNode);
        } else {
            Element element = Element.get(tree.getNodeById(assignedId));

            attachElement(feature, element, index,
                    tree.getNodeById(requestedId), requestedId == assignedId);
        }

        return Optional.empty();
    }

    private void attachElement(AttachExistingElementFeature feature,
            Element element, int index, StateNode node, boolean insertChild) {
        ChildElementConsumer callback = feature.getCallback(node);

        if (callback != null) {
            Node<?> parent = feature.getParent(node);
            feature.unregister(node);

            if (insertChild) {
                parent.insertChild(index, element);
            }
            callback.accept(element);
        }
    }

}
