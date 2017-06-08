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

import java.util.Optional;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.nodefeature.AttachTemplateChildFeature;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * RPC handler for a client-side response on attach existing element by id
 * request.
 *
 * @see JsonConstants#RPC_ATTACH_EXISTING_ELEMENT_BY_ID
 * @see JsonConstants#RPC_ATTACH_ASSIGNED_ID
 * @see JsonConstants#RPC_ATTACH_REQUESTED_ID
 * @see JsonConstants#RPC_ATTACH_INDEX
 * @see JsonConstants#RPC_ATTACH_TAG_NAME
 *
 * @author Vaadin Ltd
 */
public class AttachTemplateChildRpcHandler
        extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_ATTACH_EXISTING_ELEMENT_BY_ID;
    }

    @Override
    protected void handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_REQUESTED_ID);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_ASSIGNED_ID);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_TAG_NAME);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_ID);

        int requestedId = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_REQUESTED_ID);
        int assignedId = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_ASSIGNED_ID);

        AttachTemplateChildFeature feature = node
                .getFeature(AttachTemplateChildFeature.class);

        StateTree tree = (StateTree) node.getOwner();
        StateNode requestedNode = tree.getNodeById(requestedId);

        Element parent = feature.getParent(requestedNode);
        if (assignedId == -1) {
            String tag = invocationJson
                    .getString(JsonConstants.RPC_ATTACH_TAG_NAME);
            String id = invocationJson.getString(JsonConstants.RPC_ATTACH_ID);

            feature.unregister(requestedNode);

            if (id == null) {
                throw new IllegalStateException(String.format(
                        "The element with the tag name '%s' was "
                                + "not found in the parent with id='%d'",
                        tag, parent.getNode().getId()));
            } else {
                throw new IllegalStateException(String.format(
                        "The element with the tag name '%s' and id '%s' was "
                                + "not found in the parent with id='%d'",
                        tag, id, parent.getNode().getId()));
            }

        } else {
            StateNode elementNode = tree.getNodeById(assignedId);

            if (requestedId == assignedId) {
                feature.unregister(elementNode);
                Element element = Element.get(elementNode);

                Optional<ShadowRoot> shadowRoot = parent.getShadowRoot();
                if (shadowRoot.isPresent()) {
                    shadowRoot.get().insertVirtualChild(element);
                }
            } else {
                feature.unregister(tree.getNodeById(requestedId));
            }
        }
    }

}
