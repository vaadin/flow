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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.TemplateElementStateProviderTest;
import com.vaadin.flow.template.angular.InlineTemplate;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.AngularTemplate;
import com.vaadin.ui.ComponentTest.TestComponent;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 *
 */
public class EventRpcHandlerTest {

    @Test
    public void templateRootElementEventListener() throws Exception {
        UI ui = new UI();
        AngularTemplate t = new InlineTemplate("<root><child></child></root>");
        Element element = t.getElement();
        ui.add(t);
        AtomicInteger invoked = new AtomicInteger(0);
        element.addEventListener("test-event", e -> {
            invoked.incrementAndGet();
        });
        sendElementEvent(element, ui, "test-event", null);
        assertEquals(1, invoked.get());
    }

    @Test
    public void templateChildElementEventListener() throws Exception {
        UI ui = new UI();
        AngularTemplate t = new InlineTemplate("<root><child></child></root>");
        Element element = t.getElement().getChild(0);
        ui.add(t);
        AtomicInteger invoked = new AtomicInteger(0);
        element.addEventListener("test-event", e -> {
            invoked.incrementAndGet();
        });
        sendElementEvent(element, ui, "test-event", null);
        assertEquals(1, invoked.get());
    }

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
        assertEquals(1, invocations.get());
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
        assertEquals(123, invocationData.get());
    }

    public static StateNode getInvocationNode(Element element) {
        return TemplateElementStateProviderTest.getOverrideNode(element)
                .orElse(element.getNode());
    }

    private static JsonObject createElementEventInvocation(Element element,
            String eventType, JsonObject eventData) {
        StateNode node = getInvocationNode(element);
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
