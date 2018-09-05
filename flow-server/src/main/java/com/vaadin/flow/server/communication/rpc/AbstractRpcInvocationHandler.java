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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Abstract invocation handler implementation with common methods.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class AbstractRpcInvocationHandler
        implements RpcInvocationHandler {

    @Override
    public Optional<Runnable> handle(UI ui, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_NODE);
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));
        if (node == null) {
            getLogger().warn("Got an RPC for non-existent node: {}",
                    getNodeId(invocationJson));
            return Optional.empty();
        }
        if (!node.isAttached()) {
            getLogger().warn("Got an RPC for detached node: {}",
                    getNodeId(invocationJson));
            return Optional.empty();
        }

        if (node.isInactive()) {
            // ignore RPC requests from the client side for the nodes that are
            // invisible or disabled
            LoggerFactory.getLogger(AbstractRpcInvocationHandler.class).trace(
                    "RPC request for invocation handler '{}' is recieved from "
                            + "the client side for inactive node id='{}'",
                    getClass().getName(), node.getId());
            return Optional.empty();
        } else {
            return handleNode(node, invocationJson);
        }
    }

    /**
     * Handle the RPC data {@code invocationJson} using target {@code node} as a
     * context.
     *
     * @param node
     *            node to handle invocation with, not {@code null}
     * @param invocationJson
     *            the RPC data to handle, not {@code null}
     * @return an optional runnable
     */
    protected abstract Optional<Runnable> handleNode(StateNode node,
            JsonObject invocationJson);

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(AbstractRpcInvocationHandler.class.getName());
    }

    private static int getNodeId(JsonObject invocationJson) {
        return (int) invocationJson.getNumber(JsonConstants.RPC_NODE);
    }
}
