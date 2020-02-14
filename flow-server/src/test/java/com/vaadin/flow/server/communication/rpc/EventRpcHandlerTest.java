/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

public class EventRpcHandlerTest {

    @Test
    public void testElementEventNoData() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        UI ui = new UI();
        ui.add(c);
        AtomicInteger invocations = new AtomicInteger(0);

        element.addEventListener("test-event",
                e -> invocations.incrementAndGet());
        sendElementEvent(element, ui, "test-event", null);
        Assert.assertEquals(1, invocations.get());
    }

    @Test
    public void testElementEventData() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        UI ui = new UI();
        ui.add(c);
        AtomicInteger invocationData = new AtomicInteger(0);

        element.addEventListener("test-event", e -> invocationData
                .addAndGet((int) e.getEventData().getNumber("nr")));
        JsonObject eventData = Json.createObject();
        eventData.put("nr", 123);
        sendElementEvent(element, ui, "test-event", eventData);
        Assert.assertEquals(123, invocationData.get());
    }

    private static JsonObject createElementEventInvocation(Element element,
            String eventType, JsonObject eventData) {
        StateNode node = element.getNode();
        // Copied from ServerConnector
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_EVENT_TYPE, eventType);

        if (eventData != null) {
            message.put(JsonConstants.RPC_EVENT_DATA, eventData);
        }

        return message;
    }

    private static void sendElementEvent(Element element, UI ui,
            String eventType, JsonObject eventData) throws Exception {
        new EventRpcHandler().handle(ui,
                createElementEventInvocation(element, eventType, eventData));
    }
}
