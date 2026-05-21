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
package com.vaadin.client.communication;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Handles creating and sending messages to the server. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/ServerConnector.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "ServerConnector")
public class ServerConnector {

    public ServerConnector(
            LoadingIndicatorStateHandler loadingIndicatorStateHandler,
            ServerRpcQueue rpcQueue) {
        // Defined by the TS class constructor.
    }

    /**
     * Sends a navigation message to server.
     */
    public native void sendNavigationMessage(String location,
            Object stateObject, boolean routerLinkEvent);

    /**
     * Sends an event message to the server.
     */
    public native void sendEventMessage(StateNode node, String eventType,
            JsonObject eventData);

    /**
     * Sends an event message to the server identified by node id.
     */
    public native void sendEventMessageByNodeId(int nodeId, String eventType,
            JsonObject eventData);

    /**
     * Sends a template event message to the server.
     */
    public native void sendTemplateEventMessage(StateNode node,
            String methodName, JsonArray argsArray, int promiseId);

    /**
     * Sends a node value sync message to the server.
     */
    public native void sendNodeSyncMessage(StateNode node, int feature,
            String key, Object value);

    /**
     * Sends an attach-existing-element message to the server.
     */
    public native void sendExistingElementAttachToServer(StateNode parent,
            int requestedId, int assignedId, String tagName, int index);

    /**
     * Sends an attach-existing-element-by-id message to the server.
     */
    public native void sendExistingElementWithIdAttachToServer(StateNode parent,
            int requestedId, int assignedId, String id);

    /**
     * Sends a return channel message to the server.
     */
    public native void sendReturnChannelMessage(int stateNodeId, int channelId,
            JsonArray arguments);
}
