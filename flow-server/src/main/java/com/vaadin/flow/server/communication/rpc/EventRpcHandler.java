/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
    public Optional<Runnable> handleNode(StateNode node,
            JsonObject invocationJson) {
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
