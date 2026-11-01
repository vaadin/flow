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
import java.util.concurrent.atomic.AtomicReference;

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
    void connect_sendsCommentToOpenStream() throws Exception {
        SseStream stream = new SseStream();

        connection.connect(stream.asyncContext);

        // The client ignores comments, but writing one sends the headers
        assertEquals(":\n\n", stream.raw());
        assertTrue(stream.events().isEmpty());
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
        assertEquals(1, events.size());
        SseEvent uidl = events.get(0);
        // Unnamed events reach the message handler of EventSource
        assertNull(uidl.event);
        // The client parses the data with JSON.parse, so it must be plain
        // JSON without any prefix or array wrapping
        JsonNode json = parse(uidl.data);
        assertTrue(json.isObject(), "UIDL should be a JSON object");
        assertTrue(json.has("syncId"), "UIDL should contain a syncId");
    }

    @Test
    void push_connected_consecutivePushesAreSeparateEvents() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        vaadinSession.runWithLock(() -> {
            connection.push();
            connection.push();
            return null;
        });

        List<SseEvent> events = stream.events();
        assertEquals(2, events.size());
        int first = parse(events.get(0).data).get("syncId").asInt();
        int second = parse(events.get(1).data).get("syncId").asInt();
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
        assertEquals(1, events.size());
        assertTrue(parse(events.get(0).data).has("syncId"));
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

        SseEvent event = stream.events().get(0);
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
        assertEquals(1, stream.events().size());
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

        connection.connectionLost(stream.asyncContext);

        assertFalse(connection.isConnected());
        assertNull(connection.getAsyncContext());
    }

    @Test
    void connectionLost_previousStream_keepsCurrentStream() throws Exception {
        SseStream previous = new SseStream();
        SseStream current = new SseStream();
        connection.connect(previous.asyncContext);
        connection.connect(current.asyncContext);

        // The end of the previous stream is reported after the client has
        // already opened a new one
        connection.connectionLost(previous.asyncContext);

        assertTrue(connection.isConnected());
        assertEquals(current.asyncContext, connection.getAsyncContext());
    }

    @Test
    void connectionLost_keepsPendingPush() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);
        stream.failWrites = true;
        vaadinSession.runWithLock(() -> {
            connection.push();
            connection.push();
            return null;
        });

        connection.connectionLost(stream.asyncContext);

        assertEquals(State.PUSH_PENDING, connection.getState());
    }

    @Test
    void sendKeepAlive_connected_sendsComment() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);

        assertTrue(connection.sendKeepAlive(stream.asyncContext));

        assertEquals(":\n\n:\n\n", stream.raw());
        assertTrue(connection.isConnected());
    }

    @Test
    void sendKeepAlive_previousStream_sendsNothing() throws Exception {
        SseStream previous = new SseStream();
        SseStream current = new SseStream();
        connection.connect(previous.asyncContext);
        connection.connect(current.asyncContext);

        assertFalse(connection.sendKeepAlive(previous.asyncContext));

        assertEquals(":\n\n", current.raw());
    }

    @Test
    void sendKeepAlive_writeFails_losesConnection() throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);
        stream.failWrites = true;

        assertFalse(connection.sendKeepAlive(stream.asyncContext));

        assertFalse(connection.isConnected());
    }

    @Test
    void push_concurrentConnectionLoss_neverFails() throws Exception {
        // Streams end on container threads without the session lock, while
        // pushes happen with it
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread pusher = new Thread(() -> {
            try {
                for (int i = 0; i < 2000; i++) {
                    vaadinSession.runWithLock(() -> {
                        connection.push();
                        return null;
                    });
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        pusher.start();
        while (pusher.isAlive()) {
            SseStream stream = new SseStream();
            vaadinSession.runWithLock(() -> {
                connection.connect(stream.asyncContext);
                return null;
            });
            connection.connectionLost(stream.asyncContext);
        }

        assertNull(failure.get());
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
