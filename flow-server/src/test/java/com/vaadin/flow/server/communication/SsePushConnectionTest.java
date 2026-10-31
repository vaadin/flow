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
package com.vaadin.flow.server.communication;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.communication.SsePushConnection.State;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SsePushConnectionTest {

    private MockVaadinSession vaadinSession;
    private UI ui;
    private SsePushConnection connection;

    @BeforeEach
    void setup() {
        vaadinSession = new MockVaadinSession();
        vaadinSession.lock();
        ui = new MockUI(vaadinSession);
        vaadinSession.unlock();
        connection = new SsePushConnection(ui);
    }

    @Test
    void newConnection_isDisconnected() {
        assertFalse(connection.isConnected());
        assertEquals(State.DISCONNECTED, connection.getState());
    }

    @Test
    void connect_setsEventStreamHeaders() throws Exception {
        SseStream stream = new SseStream();

        connection.connect(stream.asyncContext);

        verify(stream.response).setContentType("text/event-stream");
        verify(stream.response).setCharacterEncoding("UTF-8");
        verify(stream.response).setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
        verify(stream.response).setHeader("X-Accel-Buffering", "no");
        assertTrue(connection.isConnected());
        assertEquals(stream.asyncContext, connection.getAsyncContext());
    }

    @Test
    void connect_sendsConnectedEventWithUiId() throws Exception {
        SseStream stream = new SseStream();

        connection.connect(stream.asyncContext);

        List<SseEvent> events = stream.events();
        assertEquals(1, events.size());
        assertEquals("connected", events.get(0).event);
        assertEquals(ui.getUIId(),
                parse(events.get(0).data).get("uiId").asInt());
    }

    @Test
    void push_connected_sendsUidlAsPlainJson() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        vaadinSession.runWithLock(() -> {
            connection.push();
            return null;
        });

        List<SseEvent> events = stream.events();
        assertEquals(2, events.size());
        SseEvent uidl = events.get(1);
        assertEquals("uidl", uidl.event);
        // The client parses the data with JSON.parse, so it must be plain
        // JSON without any prefix or array wrapping
        JsonNode json = parse(uidl.data);
        assertTrue(json.isObject(), "UIDL should be a JSON object");
        assertTrue(json.has("syncId"), "UIDL should contain a syncId");
        assertEquals(String.valueOf(json.get("syncId").asInt()), uidl.id,
                "The event id should be the sync id of the message");
    }

    @Test
    void push_connected_consecutivePushesHaveIncreasingIds() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        vaadinSession.runWithLock(() -> {
            connection.push();
            connection.push();
            return null;
        });

        List<SseEvent> events = stream.events();
        assertEquals(3, events.size());
        int first = Integer.parseInt(events.get(1).id);
        int second = Integer.parseInt(events.get(2).id);
        assertEquals(first + 1, second);
    }

    @Test
    void push_disconnected_isDeferredUntilConnect() throws Exception {
        vaadinSession.runWithLock(() -> {
            connection.push();
            return null;
        });
        assertEquals(State.PUSH_PENDING, connection.getState());

        SseStream stream = new SseStream();
        vaadinSession.runWithLock(() -> {
            connection.connect(stream.asyncContext);
            return null;
        });

        List<SseEvent> events = stream.events();
        assertEquals(2, events.size());
        assertEquals("connected", events.get(0).event);
        assertEquals("uidl", events.get(1).event);
        assertTrue(connection.isConnected());
    }

    @Test
    void push_responsePending_isNotDowngradedByAsyncPush() throws Exception {
        vaadinSession.runWithLock(() -> {
            connection.push(false);
            connection.push(true);
            return null;
        });

        assertEquals(State.RESPONSE_PENDING, connection.getState());
    }

    @Test
    void sendMessage_multiLineMessage_eachLineIsDataField() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        connection.sendMessage("first\nsecond\n\nfourth");

        SseEvent event = stream.events().get(1);
        assertEquals("first\nsecond\n\nfourth", event.data);
        assertTrue(stream.raw().contains(
                "data: first\ndata: second\ndata: \ndata: fourth\n\n"));
    }

    @Test
    void sendMessage_writerError_losesConnection() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);
        stream.failWrites = true;

        connection.sendMessage("{}");

        assertFalse(connection.isConnected());
        assertEquals(State.DISCONNECTED, connection.getState());
        assertNull(connection.getAsyncContext());
    }

    @Test
    void push_afterWriterError_isDeferredToNextConnection() throws Exception {
        SseStream broken = new SseStream();
        connection.connect(broken.asyncContext);
        broken.failWrites = true;
        vaadinSession.runWithLock(() -> {
            connection.push();
            return null;
        });
        assertFalse(connection.isConnected());

        vaadinSession.runWithLock(() -> {
            connection.push();
            return null;
        });
        assertEquals(State.PUSH_PENDING, connection.getState());

        SseStream stream = new SseStream();
        vaadinSession.runWithLock(() -> {
            connection.connect(stream.asyncContext);
            return null;
        });
        assertEquals("uidl", stream.events().get(1).event);
    }

    @Test
    void disconnect_completesAsyncContext() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        connection.disconnect();

        verify(stream.asyncContext).complete();
        assertFalse(connection.isConnected());
        assertNull(connection.getAsyncContext());
    }

    @Test
    void disconnect_notConnected_isIgnored() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);
        connection.disconnect();

        connection.disconnect();

        verify(stream.asyncContext, times(1)).complete();
    }

    @Test
    void connect_alreadyConnected_closesPreviousStream() throws Exception {
        SseStream first = new SseStream();
        SseStream second = new SseStream();
        connection.connect(first.asyncContext);

        connection.connect(second.asyncContext);

        verify(first.asyncContext).complete();
        verify(second.asyncContext, never()).complete();
        assertTrue(connection.isConnected());
        assertEquals(second.asyncContext, connection.getAsyncContext());
    }

    @Test
    void connectionLost_disconnects() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        connection.connectionLost();

        assertFalse(connection.isConnected());
        assertNull(connection.getAsyncContext());
    }

    @Test
    void connectionLost_keepsPendingPush() {
        vaadinSession.lock();
        try {
            connection.push();
        } finally {
            vaadinSession.unlock();
        }

        connection.connectionLost();

        assertEquals(State.PUSH_PENDING, connection.getState());
    }

    @Test
    void serialization_deserializedConnectionIsDisconnected() throws Exception {
        UI mockUi = mock(UI.class);
        SsePushConnection serializable = new SsePushConnection(mockUi);
        serializable.connect(new SseStream().asyncContext);
        assertEquals(State.CONNECTED, serializable.getState());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(serializable);
        SsePushConnection deserialized = (SsePushConnection) new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray())).readObject();

        assertEquals(State.DISCONNECTED, deserialized.getState());
        assertFalse(deserialized.isConnected());
    }

    private static JsonNode parse(String json) {
        return new ObjectMapper().readTree(json);
    }

    /**
     * An event parsed from the written event stream.
     */
    record SseEvent(String event, String id, String data) {
    }

    /**
     * Mocked async context whose response writes to an in-memory event stream.
     */
    static class SseStream {
        final AsyncContext asyncContext = mock(AsyncContext.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        private final StringWriter buffer = new StringWriter();
        boolean failWrites;

        SseStream() throws Exception {
            PrintWriter writer = new PrintWriter(buffer) {
                @Override
                public boolean checkError() {
                    return failWrites || super.checkError();
                }
            };
            when(asyncContext.getResponse()).thenReturn(response);
            when(response.getWriter()).thenReturn(writer);
        }

        String raw() {
            return buffer.toString();
        }

        /**
         * Parses the written stream according to the event stream format:
         * events are separated by empty lines, multiple data fields are joined
         * with newlines.
         */
        List<SseEvent> events() {
            return parse(raw());
        }

        static List<SseEvent> parse(String stream) {
            List<SseEvent> events = new ArrayList<>();
            String event = null;
            String id = null;
            List<String> data = new ArrayList<>();
            for (String line : stream.split("\n", -1)) {
                if (line.isEmpty()) {
                    if (event != null || !data.isEmpty()) {
                        events.add(new SseEvent(event, id,
                                String.join("\n", data)));
                    }
                    event = null;
                    id = null;
                    data.clear();
                } else if (line.startsWith("event: ")) {
                    event = line.substring("event: ".length());
                } else if (line.startsWith("id: ")) {
                    id = line.substring("id: ".length());
                } else if (line.startsWith("data: ")) {
                    data.add(line.substring("data: ".length()));
                } else if (line.startsWith("data:")) {
                    data.add(line.substring("data:".length()));
                }
            }
            return events;
        }
    }
}
