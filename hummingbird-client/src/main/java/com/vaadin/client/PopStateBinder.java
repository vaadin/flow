/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client;

import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.hummingbird.util.ClientJsonCodec;
import com.vaadin.shared.JsonConstants;

import elemental.client.Browser;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Helper for binding <code>popstate</code> events to the server.
 *
 * @since
 * @author Vaadin Ltd
 */
public class PopStateBinder {

    private PopStateBinder() {
        // Only static functionality
    }

    /**
     * Sets up a <code>popstate</code> listener for delivering events to the
     * server.
     *
     * @param rpcQueue
     *            the RPC queue used for sending the events
     */
    public static void bind(ServerRpcQueue rpcQueue) {
        Browser.getWindow().setOnpopstate(e -> {
            Object stateObject = WidgetUtil.getJsProperty(e, "state");
            String location = Browser.getWindow().getLocation().getHref();

            JsonObject invocation = Json.createObject();
            invocation.put(JsonConstants.RPC_TYPE,
                    JsonConstants.RPC_TYPE_POPSTATE);
            invocation.put(JsonConstants.RPC_POPSTATE_LOCATION, location);
            if (stateObject != null) {
                JsonValue stateJson = ClientJsonCodec
                        .encodeWithoutTypeInfo(stateObject);
                invocation.put(JsonConstants.RPC_POPSTATE_STATE, stateJson);
            }

            rpcQueue.add(invocation);
            rpcQueue.flush();
        });
    }

}
