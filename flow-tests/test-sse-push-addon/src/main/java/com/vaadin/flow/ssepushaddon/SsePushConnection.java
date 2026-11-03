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
package com.vaadin.flow.ssepushaddon;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.UidlWriter;

/**
 * A {@link PushConnection} implementation using Server-Sent Events (SSE)
 * without the Atmosphere framework.
 * <p>
 * SSE is used for server-to-client communication while XHR is used for
 * client-to-server messages (similar to the WEBSOCKET_XHR transport pattern).
 * <p>
 * Part of the SSE push transport add-on, see {@link SsePushTransport}.
 *
 * @author Vaadin Ltd
 */
public class SsePushConnection implements PushConnection {

    /**
     * Connection states for SSE push.
     */
    protected enum State {
        /**
         * Not connected. Trying to push will set the connection state to
         * PUSH_PENDING or RESPONSE_PENDING and defer sending the message until
         * a connection is established.
         */
        DISCONNECTED,

        /**
         * Not connected. An asynchronous push is pending the opening of the
         * connection.
         */
        PUSH_PENDING,

        /**
         * Not connected. A response to a client request is pending the opening
         * of the connection.
         */
        RESPONSE_PENDING,

        /**
         * Connected. Messages can be sent through the connection.
         */
        CONNECTED
    }

    /**
     * An event that tells the client how many seconds to expect between keep
     * alive events, 0 if none are sent. It commits the response headers when
     * the stream opens and keeps the stream from being closed as idle. Not a
     * comment, as EventSource does not pass those on, and the client needs to
     * notice a stream that has died silently.
     */
    private static final String KEEP_ALIVE = "event: keep-alive\ndata: %d\n\n";

    private final UI ui;
    private transient volatile State state = State.DISCONNECTED;
    private transient EventStream stream;
    private transient int keepAliveInterval;
    // The executor of the service, unless given for tests
    private final transient Executor executor;
    // Not synchronized: writes are queued and carried out without this lock,
    // but when the service has no executor they are carried out right away,
    // and may then wait for a slow client with this lock held. On Java 21 to
    // 23 a virtual thread waiting inside synchronized pins its carrier thread
    // (fixed in Java 24 by JEP 491).
    private transient ReentrantLock lock = new ReentrantLock();

    /**
     * Creates an instance connected to the given UI.
     *
     * @param ui
     *            the UI to which this connection belongs
     */
    public SsePushConnection(UI ui) {
        this(ui, null);
    }

    /**
     * Creates an instance that writes to the client with the given executor.
     *
     * @param ui
     *            the UI to which this connection belongs
     * @param executor
     *            the executor to write with, or {@code null} for the executor
     *            of the service
     */
    SsePushConnection(UI ui, Executor executor) {
        this.ui = ui;
        this.executor = executor;
    }

    @Override
    public void push() {
        push(true);
    }

    /**
     * Pushes pending state changes and client RPC calls to the client. If
     * {@code isConnected()} is false, defers the push until a connection is
     * established.
     *
     * @param async
     *            True if this push asynchronously originates from the server,
     *            false if it is a response to a client request.
     */
    public void push(boolean async) {
        lock.lock();
        try {
            if (!isConnected()) {
                if (async && state != State.RESPONSE_PENDING) {
                    state = State.PUSH_PENDING;
                } else {
                    state = State.RESPONSE_PENDING;
                }
                return;
            }
            try {
                sendMessage(
                        new UidlWriter().createUidl(getUI(), async).toString());
            } catch (Exception e) {
                throw new RuntimeException("Push failed", e);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sends the given message to the current client as an event. Cannot be
     * called if {@link #isConnected()} returns false.
     *
     * @param message
     *            The message to send
     */
    protected void sendMessage(String message) {
        assert isConnected() : "Cannot send message when not connected";
        write(toEvent(message));
    }

    /**
     * Formats the given data as an event of the event stream. Each line of the
     * data goes to a data field of its own.
     *
     * @param data
     *            the data of the event
     * @return the event, ending with the empty line that ends an event
     */
    static String toEvent(String data) {
        StringBuilder event = new StringBuilder();
        for (String line : data.split("\n", -1)) {
            event.append("data: ").append(line).append('\n');
        }
        return event.append('\n').toString();
    }

    /**
     * Sends a keep-alive event to the client through the given stream, unless
     * the connection uses another stream by now. Proxies and load balancers
     * close connections that stay idle, and writing to a stream is also how the
     * server notices that the client has gone.
     *
     * @param asyncContext
     *            the stream to keep alive
     * @return {@code true} if the stream is still the open stream of this
     *         connection, {@code false} if it has ended
     */
    boolean sendKeepAlive(AsyncContext asyncContext) {
        lock.lock();
        try {
            if (!isConnected() || stream.context != asyncContext) {
                return false;
            }
            write(keepAliveEvent());
            // A failed write is noticed here when written right away,
            // otherwise at the next keep-alive
            return isConnected();
        } finally {
            lock.unlock();
        }
    }

    private String keepAliveEvent() {
        return KEEP_ALIVE.formatted(keepAliveInterval);
    }

    private void write(String text) {
        stream.write(text);
    }

    @Override
    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    /**
     * Associates this {@code SsePushConnection} with the given
     * {@link AsyncContext} representing an established SSE connection. If
     * already connected, calls {@link #disconnect()} first. If there is a
     * deferred push, carries it out via the new connection.
     *
     * @param asyncContext
     *            the async context to associate this connection with
     * @throws IOException
     *             if setting up the SSE connection fails
     */
    public void connect(AsyncContext asyncContext) throws IOException {
        EventStream previous = connect(asyncContext, 0);
        if (previous != null) {
            previous.complete();
        }
    }

    /**
     * Associates this connection with the given stream, telling the client to
     * expect keep-alive events at the given interval.
     *
     * @param asyncContext
     *            the async context to associate this connection with
     * @param keepAliveInterval
     *            the seconds between keep-alive events, 0 if none are sent
     * @return the stream this connection used before, or {@code null}. The
     *         caller completes it once it no longer holds the session lock.
     * @throws IOException
     *             if setting up the SSE connection fails
     */
    EventStream connect(AsyncContext asyncContext, int keepAliveInterval)
            throws IOException {
        assert asyncContext != null;

        EventStream previous = null;
        lock.lock();
        try {
            if (isConnected()) {
                previous = detach();
            }

            HttpServletResponse response = (HttpServletResponse) asyncContext
                    .getResponse();
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control",
                    "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Connection", "keep-alive");
            // Stops nginx from buffering the stream
            response.setHeader("X-Accel-Buffering", "no");

            stream = new EventStream(asyncContext, response.getWriter());

            State oldState = state;
            state = State.CONNECTED;

            this.keepAliveInterval = keepAliveInterval;
            // Sends the headers, which opens the stream on the client. Written
            // right away, as nothing else is written to a new stream yet.
            stream.writeNow(keepAliveEvent());

            if (oldState == State.PUSH_PENDING
                    || oldState == State.RESPONSE_PENDING) {
                // Sending a "response" message (async=false) also takes care of
                // a pending push, but not vice versa
                push(oldState == State.PUSH_PENDING);
            }
        } finally {
            lock.unlock();
        }
        return previous;
    }

    /**
     * Gets the UI associated with this connection.
     *
     * @return the UI associated with this connection.
     */
    protected UI getUI() {
        return ui;
    }

    /**
     * Gets the AsyncContext associated with this connection.
     *
     * @return The AsyncContext associated with this connection or null if
     *         connection not open.
     */
    protected AsyncContext getAsyncContext() {
        EventStream current = stream;
        return current != null ? current.context : null;
    }

    @Override
    public void disconnect() {
        EventStream ended;
        lock.lock();
        try {
            if (!isConnected()) {
                getLogger().debug(
                        "Disconnection already happened, ignoring request");
                return;
            }
            ended = detach();
        } finally {
            lock.unlock();
        }
        ended.complete();
    }

    /**
     * Detaches the current stream, so that its end is not reported as a lost
     * connection. Must be called with the lock held.
     *
     * @return the detached stream, to complete once the lock is released
     */
    private EventStream detach() {
        EventStream detached = stream;
        connectionLost(detached.context);
        return detached;
    }

    /**
     * Called when the given event stream has ended. Does nothing if the
     * connection has already moved on to another stream, as the client may open
     * a new stream before the server notices that the previous one ended.
     *
     * @param asyncContext
     *            the async context of the stream that ended
     */
    public void connectionLost(AsyncContext asyncContext) {
        lock.lock();
        try {
            if (stream == null || asyncContext != stream.context) {
                return;
            }
            stream = null;
            if (state == State.CONNECTED) {
                state = State.DISCONNECTED;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the state of this connection.
     *
     * @return the state of this connection
     */
    protected State getState() {
        return state;
    }

    /**
     * Reinitializes this PushConnection after deserialization. The connection
     * is initially in disconnected state; the client will handle the
     * reconnecting.
     *
     * @param stream
     *            the object to read
     * @throws IOException
     *             if an IO error occurred
     * @throws ClassNotFoundException
     *             if the class of the stream object could not be found
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        state = State.DISCONNECTED;
        lock = new ReentrantLock();
    }

    private Executor getExecutor() {
        if (executor != null) {
            return executor;
        }
        VaadinSession session = ui.getSession();
        Executor serviceExecutor = session != null
                ? session.getService().getExecutor()
                : null;
        return serviceExecutor != null ? serviceExecutor : Runnable::run;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SsePushConnection.class.getName());
    }

    /**
     * An open event stream. Writes and completing the stream are queued and
     * carried out in order on the executor of the service, so that a push, made
     * with the session locked, never waits for a slow client: the session would
     * stay locked until the container gives up on the write, see
     * https://github.com/vaadin/flow/issues/12316
     */
    final class EventStream implements Serializable {
        private final transient AsyncContext context;
        private final transient PrintWriter writer;
        private final transient Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private final transient AtomicBoolean draining = new AtomicBoolean();

        private EventStream(AsyncContext context, PrintWriter writer) {
            this.context = context;
            this.writer = writer;
        }

        private void write(String text) {
            submit(() -> writeNow(text));
        }

        /**
         * Ends this stream once the writes queued before have been carried out.
         * Called without the lock of the connection, and when possible without
         * the session lock: the container may need the HTTP session lock to
         * complete the stream, while a thread invalidating the session holds
         * that lock and waits for this connection or for the session, see
         * https://github.com/vaadin/flow/issues/16293
         */
        void complete() {
            submit(() -> {
                try {
                    context.complete();
                } catch (Exception e) {
                    getLogger().debug("Error when completing async context", e);
                }
            });
        }

        private void writeNow(String text) {
            writer.write(text);
            writer.flush();
            // PrintWriter does not throw, but reports errors here
            if (writer.checkError()) {
                getLogger().debug("Writing to the event stream failed");
                connectionLost(context);
            }
        }

        private void submit(Runnable action) {
            queue.add(action);
            if (draining.compareAndSet(false, true)) {
                try {
                    getExecutor().execute(this::drain);
                } catch (RuntimeException e) {
                    // E.g. an executor that is shutting down
                    drain();
                }
            }
        }

        private void drain() {
            do {
                Runnable action;
                while ((action = queue.poll()) != null) {
                    try {
                        action.run();
                    } catch (RuntimeException e) {
                        getLogger().debug("Writing to the event stream failed",
                                e);
                        connectionLost(context);
                    }
                }
                draining.set(false);
                // Another thread may have queued an action after the queue
                // was found empty, but before draining was set to false
            } while (!queue.isEmpty() && draining.compareAndSet(false, true));
        }
    }
}
