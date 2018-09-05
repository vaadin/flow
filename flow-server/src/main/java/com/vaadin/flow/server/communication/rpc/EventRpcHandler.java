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

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * RPC handler for events.
 * 
 * @see JsonConstants#RPC_EVENT_TYPE
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class EventRpcHandler extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_EVENT;
    }

    @Override
    public Optional<Runnable> handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_EVENT_TYPE);

        String eventType = invocationJson
                .getString(JsonConstants.RPC_EVENT_TYPE);

        JsonObject eventData = invocationJson
                .getObject(JsonConstants.RPC_EVENT_DATA);
        if (eventData == null) {
            eventData = Json.createObject();
        }

        DomEvent event = new DomEvent(Element.get(node), eventType, eventData);

        node.getFeature(ElementListenerMap.class).fireEvent(event);

        return Optional.empty();
    }

}
