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

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.server.communication.MethodInvocationUtil;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.Component;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * RPC handler for events triggered through <code>element.$server</code> or
 * simply <code>$server</code> in template event handlers.
 *
 * @see JsonConstants#RPC_PUBLISHED_SERVER_EVENT_HANDLER
 *
 * @author Vaadin Ltd
 *
 */
public class PublishedServerEventHandlerRpcHandler
        extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_PUBLISHED_SERVER_EVENT_HANDLER;
    }

    @Override
    public void handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson
                .hasKey(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);
        String methodName = invocationJson
                .getString(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);
        if (methodName == null) {
            throw new IllegalArgumentException(
                    "Event handler method name may not be null");
        }
        JsonValue args = invocationJson
                .get(JsonConstants.RPC_TEMPLATE_EVENT_ARGS);
        if (args == null) {
            args = Json.createArray();
        }
        if (args.getType() != JsonType.ARRAY) {
            throw new IllegalArgumentException(
                    "Incorrect type for method arguments :" + args.getClass());
        }
        assert node.hasFeature(ComponentMapping.class);
        Optional<Component> component = node.getFeature(ComponentMapping.class)
                .getComponent();
        if (!component.isPresent()) {
            throw new IllegalStateException(
                    "Unable to handle RPC template event JSON message: "
                            + "there is no component available for the target node.");
        }

        MethodInvocationUtil.invokeMethod(component.get(),
                component.get().getClass(), methodName, (JsonArray) args);

    }

}
