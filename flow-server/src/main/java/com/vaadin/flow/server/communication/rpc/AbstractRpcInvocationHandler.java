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
package com.vaadin.flow.server.communication.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

/**
 * Abstract invocation handler implementation with common methods.
 * 
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractRpcInvocationHandler
        implements RpcInvocationHandler {

    @Override
    public void handle(UI ui, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_NODE);
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));
        if (node == null) {
            getLogger().warn("Got an RPC for non-existent node: {}",
                    getNodeId(invocationJson));
            return;
        }
        if (!node.isAttached()) {
            getLogger().warn("Got an RPC for detached node: {}",
                    getNodeId(invocationJson));
            return;
        }
        handleNode(node, invocationJson);
    }

    protected abstract void handleNode(StateNode node,
            JsonObject invocationJson);

    private static Logger getLogger() {
        return LoggerFactory.getLogger(AbstractRpcInvocationHandler.class.getName());
    }

    private static int getNodeId(JsonObject invocationJson) {
        return (int) invocationJson.getNumber(JsonConstants.RPC_NODE);
    }
}
