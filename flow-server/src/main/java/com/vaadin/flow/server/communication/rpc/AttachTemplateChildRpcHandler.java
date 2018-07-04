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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonNull;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * RPC handler for a client-side response on attach existing element by id
 * request.
 *
 * @see JsonConstants#RPC_ATTACH_EXISTING_ELEMENT_BY_ID
 * @see JsonConstants#RPC_ATTACH_ASSIGNED_ID
 * @see JsonConstants#RPC_ATTACH_REQUESTED_ID
 * @see JsonConstants#RPC_ATTACH_ID
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AttachTemplateChildRpcHandler
        extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_ATTACH_EXISTING_ELEMENT_BY_ID;
    }

    @Override
    protected Optional<Runnable> handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_REQUESTED_ID);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_ASSIGNED_ID);
        assert invocationJson.hasKey(JsonConstants.RPC_ATTACH_ID);

        int requestedId = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_REQUESTED_ID);
        int assignedId = (int) invocationJson
                .getNumber(JsonConstants.RPC_ATTACH_ASSIGNED_ID);

        StateTree tree = (StateTree) node.getOwner();
        StateNode requestedNode = tree.getNodeById(requestedId);

        StateNode parent = tree.getNodeById(requestedId).getParent();
        JsonValue id = invocationJson.get(JsonConstants.RPC_ATTACH_ID);
        String tag = requestedNode.getFeature(ElementData.class).getTag();

        Logger logger = LoggerFactory
                .getLogger(AttachTemplateChildRpcHandler.class.getName());

        if (assignedId == -1) {
            logger.error("Attach existing element has failed because "
                    + "the client-side element is not found");
            if (id instanceof JsonNull) {
                throw new IllegalStateException(String.format(
                        "The element with the tag name '%s' was "
                                + "not found in the parent with id='%d'",
                        tag, parent.getId()));
            } else {
                throw new IllegalStateException(String.format(
                        "The element with the tag name '%s' and id '%s' was "
                                + "not found in the parent with id='%d'",
                        tag, id.asString(), parent.getId()));
            }
        } else if (requestedId != assignedId) {
            logger.error("Attach existing element has failed because "
                    + "the element has been already attached from the server side");
            if (id instanceof JsonNull) {
                throw new IllegalStateException(String.format(
                        "The element with the tag name '%s' is already "
                                + "attached to the parent with id='%d'",
                        tag, parent.getId()));
            } else {
                throw new IllegalStateException(String.format(
                        "The element with the tag name '%s' and id '%s' is "
                                + "already attached to the parent with id='%d'",
                        tag, id.asString(), parent.getId()));
            }
        } else {
            logger.error("Attach existing element request succeded. "
                    + "But the response about this is unexpected");

            // This should not happen. In case of successful request the client
            // side should not respond
            throw new IllegalArgumentException(
                    "Unexpected successful attachment "
                            + "response is received from the client-side. "
                            + "Client side should not respond if everything is fine");
        }
    }

}
