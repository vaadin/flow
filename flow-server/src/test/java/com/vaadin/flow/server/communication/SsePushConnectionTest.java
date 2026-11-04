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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

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
import static org.mockito.Mockito.doAnswer;
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
        // Writes right away, unless a test needs the executor of the service
        connection = new SsePushConnection(ui, Runnable::run);
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
    void connect_sendsKeepAliveToOpenStream() throws Exception {
        SseStream stream = new SseStream();

        connection.connect(stream.asyncContext, 30);

        // Writing it sends the headers, and tells the client how often to
        // expect one
        assertEquals(keepAlive(30), stream.raw());
        assertTrue(stream.events().isEmpty());
    }

    @Test
    void connect_withoutKeepAlive_tellsClientNotToExpectOne() throws Exception {
        SseStream stream = new SseStream();

        connection.connect(stream.asyncContext);

        assertEquals(keepAlive(0), stream.raw());
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

        assertEquals(keepAlive(0).repeat(2), stream.raw());
        assertTrue(connection.isConnected());
    }

    @Test
    void sendKeepAlive_previousStream_sendsNothing() throws Exception {
        SseStream previous = new SseStream();
        SseStream current = new SseStream();
        connection.connect(previous.asyncContext);
        connection.connect(current.asyncContext);

        assertFalse(connection.sendKeepAlive(previous.asyncContext));

        assertEquals(keepAlive(0), current.raw());
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
    void sendKeepAlive_stalledStreams_otherVirtualThreadsRun()
            throws Exception {
        // Writes waiting for slow clients run on the virtual threads of the
        // service executor, and must not keep their carrier threads from other
        // virtual threads. Fails only on Java 21 to 23 if a write waits inside
        // synchronized, as Java 24 no longer pins virtual threads there (JEP
        // 491).
        int carriers = Integer.getInteger(
                "jdk.virtualThreadScheduler.parallelism",
                Runtime.getRuntime().availableProcessors());
        CountDownLatch clientReads = new CountDownLatch(1);
        try {
            for (int i = 0; i < carriers; i++) {
                SsePushConnection slowConnection = new SsePushConnection(ui);
                SseStream slowStream = new SseStream();
                slowConnection.connect(slowStream.asyncContext);
                slowStream.stall = clientReads;
                slowConnection.sendKeepAlive(slowStream.asyncContext);
                assertTrue(slowStream.stalled.await(5, TimeUnit.SECONDS));
            }

            CountDownLatch ran = new CountDownLatch(1);
            Thread.ofVirtual().start(ran::countDown);

            assertTrue(ran.await(5, TimeUnit.SECONDS),
                    "Virtual threads should run while streams are stalled");
        } finally {
            clientReads.countDown();
        }
    }

    @Test
    void push_stalledClient_doesNotHoldSessionLock() throws Exception {
        // A client that stops reading must not keep the session locked until
        // the container gives up on the write, as everything else in the
        // session, including expiring it, waits for the lock, see
        // https://github.com/vaadin/flow/issues/12316
        SsePushConnection writingOnService = new SsePushConnection(ui);
        SseStream stream = new SseStream();
        writingOnService.connect(stream.asyncContext);
        CountDownLatch clientReads = new CountDownLatch(1);
        stream.stall = clientReads;
        Thread pusher = Thread.ofVirtual().start(
                () -> vaadinSession.access(() -> writingOnService.push()));
        try {
            pusher.join(2000);
            Future<Void> other = vaadinSession.access(() -> {
            });
            other.get(2, TimeUnit.SECONDS);
        } finally {
            clientReads.countDown();
            pusher.join();
        }
        long deadline = System.currentTimeMillis() + 2000;
        while (stream.events().isEmpty()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(1, stream.events().size(),
                "The message should be written once the client reads");
    }

    @Test
    void disconnect_concurrentRequests_preventDeadlocks() throws Exception {
        // When the HTTP session is invalidated, the container may hold a lock
        // on it while the UIs are closed and their push connections
        // disconnected on that thread. If another thread disconnects at the
        // same time, completing the stream may need the same HTTP session
        // lock, as with Jetty in https://github.com/vaadin/flow/issues/16293
        ReentrantLock httpSessionLock = new ReentrantLock();
        SseStream stream = streamCompletingWithLock(httpSessionLock);
        CompletableFuture<Void> otherThread;
        httpSessionLock.lock();
        try {
            otherThread = CompletableFuture.runAsync(connection::disconnect);
            stream.completing.await(2, TimeUnit.SECONDS);

            connection.disconnect();
        } finally {
            httpSessionLock.unlock();
        }

        otherThread.get(5, TimeUnit.SECONDS);
        verify(stream.asyncContext, times(1)).complete();
    }

    @Test
    void pushWhileDisconnect_preventDeadlocks() throws Exception {
        // As above, but the thread holding the HTTP session lock pushes,
        // e.g. when the Vaadin session is unlocked after it has been closed
        ReentrantLock httpSessionLock = new ReentrantLock();
        SseStream stream = streamCompletingWithLock(httpSessionLock);
        CompletableFuture<Void> otherThread;
        httpSessionLock.lock();
        try {
            otherThread = CompletableFuture.runAsync(connection::disconnect);
            stream.completing.await(2, TimeUnit.SECONDS);

            vaadinSession.runWithLock(() -> {
                connection.push();
                return null;
            });
        } finally {
            httpSessionLock.unlock();
        }

        otherThread.get(5, TimeUnit.SECONDS);
        assertEquals(State.PUSH_PENDING, connection.getState());
    }

    /**
     * Connects a stream whose completion needs the given lock, like a container
     * that accesses the HTTP session when an async request completes. Fails
     * instead of waiting forever if the lock is not released.
     */
    private SseStream streamCompletingWithLock(ReentrantLock lock)
            throws Exception {
        SseStream stream = new SseStream();
        connection.connect(stream.asyncContext);
        doAnswer(invocation -> {
            stream.completing.countDown();
            if (!lock.tryLock(2, TimeUnit.SECONDS)) {
                throw new AssertionError("Deadlock on AsyncContext.complete");
            }
            lock.unlock();
            return null;
        }).when(stream.asyncContext).complete();
        return stream;
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
     * Returns the keep-alive event with the given interval as written to the
     * stream.
     */
    static String keepAlive(int interval) {
        return "event: keep-alive\ndata: " + interval + "\n\n";
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
        // Writes wait for this, like for a client that does not read
        volatile CountDownLatch stall;
        // Counted down when a write starts to wait for the stall
        final CountDownLatch stalled = new CountDownLatch(1);
        final CountDownLatch completing = new CountDownLatch(1);

        SseStream() throws Exception {
            PrintWriter writer = new PrintWriter(buffer) {
                @Override
                public void write(String text) {
                    CountDownLatch latch = stall;
                    if (latch != null) {
                        stalled.countDown();
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    super.write(text);
                }

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
         * with newlines. Keep-alive events are left out.
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
                    if ("keep-alive".equals(event)) {
                        event = null;
                        data.clear();
                    } else if (event != null || !data.isEmpty()) {
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
