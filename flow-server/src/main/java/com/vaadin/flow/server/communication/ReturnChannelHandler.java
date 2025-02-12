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
package com.vaadin.flow.server.communication;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.communication.rpc.AbstractRpcInvocationHandler;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * RPC handler for return channel messages.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class ReturnChannelHandler extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_CHANNEL;
    }

    @Override
    protected Optional<Runnable> handleNode(StateNode node,
            JsonNode invocationJson) {
        int channelId = invocationJson.get(JsonConstants.RPC_CHANNEL)
                .intValue();
        ArrayNode arguments = (ArrayNode) invocationJson
                .get(JsonConstants.RPC_CHANNEL_ARGUMENTS);

        if (!node.hasFeature(ReturnChannelMap.class)) {
            getLogger().warn("Node has no return channels: {}", invocationJson);
            return Optional.empty();
        }

        ReturnChannelRegistration channel = node
                .getFeatureIfInitialized(ReturnChannelMap.class)
                .map(map -> map.get(channelId)).orElse(null);

        if (channel == null) {
            getLogger().warn("Return channel not found: {}", invocationJson);
            return Optional.empty();
        }

        if (!node.isEnabled() && channel
                .getDisabledUpdateMode() != DisabledUpdateMode.ALWAYS) {
            getLogger().warn("Ignoring update for disabled return channel: {}",
                    invocationJson);
            return Optional.empty();
        }

        channel.invoke(arguments);

        return Optional.empty();
    }

    @Override
    protected boolean allowInert(UI ui, JsonNode invocationJson) {
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));
        // Allow calls if a return channel has been registered for the node.
        return node.getFeatureIfInitialized(ReturnChannelMap.class)
                .map(ReturnChannelMap::hasChannels).orElse(false);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ReturnChannelHandler.class.getName());
    }

}
