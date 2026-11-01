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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UsageStatistics;

/**
 * A {@link PushConnection} implementation using Server-Sent Events (SSE)
 * without the Atmosphere framework.
 * <p>
 * SSE is used for server-to-client communication while XHR is used for
 * client-to-server messages (similar to the WEBSOCKET_XHR transport pattern).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class SsePushConnection implements PushConnection, Serializable {

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

    private static final String SSE_VERSION = "1.0";

    /**
     * A comment line, which the client ignores. It commits the response headers
     * when the stream opens and keeps the stream from being closed as idle.
     */
    private static final String KEEP_ALIVE = ":\n\n";

    private UI ui;
    private transient volatile State state = State.DISCONNECTED;
    private transient AsyncContext asyncContext;
    private transient PrintWriter writer;
    private transient Object lock = new Object();

    /**
     * Creates an instance connected to the given UI.
     *
     * @param ui
     *            the UI to which this connection belongs
     */
    public SsePushConnection(UI ui) {
        this.ui = ui;
        UsageStatistics.markAsUsed("flow/SsePushConnection", SSE_VERSION);
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
        synchronized (lock) {
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
     * Sends a comment to the client through the given stream, unless the
     * connection uses another stream by now. Proxies and load balancers close
     * connections that stay idle, and writing to a stream is also how the
     * server notices that the client has gone.
     *
     * @param asyncContext
     *            the stream to keep alive
     * @return {@code true} if the stream is still the open stream of this
     *         connection, {@code false} if it has ended
     */
    boolean sendKeepAlive(AsyncContext asyncContext) {
        synchronized (lock) {
            if (!isConnected() || this.asyncContext != asyncContext) {
                return false;
            }
            write(KEEP_ALIVE);
            return isConnected();
        }
    }

    private void write(String text) {
        synchronized (lock) {
            writer.write(text);
            writer.flush();
            // PrintWriter does not throw, but reports errors here
            if (writer.checkError()) {
                getLogger().debug("Writing to the event stream failed");
                connectionLost(asyncContext);
            }
        }
    }

    @Override
    public boolean isConnected() {
        assert state != null;
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
        assert asyncContext != null;

        synchronized (lock) {
            if (isConnected()) {
                disconnect();
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

            this.asyncContext = asyncContext;
            this.writer = response.getWriter();

            State oldState = state;
            state = State.CONNECTED;

            // Sends the headers, which opens the stream on the client
            write(KEEP_ALIVE);

            if (oldState == State.PUSH_PENDING
                    || oldState == State.RESPONSE_PENDING) {
                // Sending a "response" message (async=false) also takes care of
                // a pending push, but not vice versa
                push(oldState == State.PUSH_PENDING);
            }
        }
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
        return asyncContext;
    }

    @Override
    public void disconnect() {
        synchronized (lock) {
            if (!isConnected()) {
                getLogger().debug(
                        "Disconnection already happened, ignoring request");
                return;
            }
            AsyncContext context = asyncContext;
            // Detached first, so that the end of the stream is not reported
            // as a lost connection
            connectionLost(context);
            try {
                context.complete();
            } catch (Exception e) {
                getLogger().debug("Error when completing async context", e);
            }
        }
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
        synchronized (lock) {
            if (asyncContext != this.asyncContext) {
                return;
            }
            this.asyncContext = null;
            writer = null;
            if (state == State.CONNECTED) {
                state = State.DISCONNECTED;
            }
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
        lock = new Object();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SsePushConnection.class.getName());
    }
}
