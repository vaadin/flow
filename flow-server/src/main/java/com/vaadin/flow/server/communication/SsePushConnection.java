/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import tools.jackson.databind.JsonNode;

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
 * @since 24.7
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

    private UI ui;
    private transient State state = State.DISCONNECTED;
    private transient AsyncContext asyncContext;
    private transient PrintWriter writer;
    private transient Object lock = new Object();
    private volatile boolean disconnecting;

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
        if (disconnecting || !isConnected()) {
            if (disconnecting) {
                getLogger().debug(
                        "Disconnection in progress, ignoring push request");
            }
            if (async && state != State.RESPONSE_PENDING) {
                state = State.PUSH_PENDING;
            } else {
                state = State.RESPONSE_PENDING;
            }
        } else {
            synchronized (lock) {
                try {
                    JsonNode response = new UidlWriter().createUidl(getUI(),
                            async);
                    sendMessage("for(;;);[" + response + "]");
                } catch (Exception e) {
                    throw new RuntimeException("Push failed", e);
                }
            }
        }
    }

    /**
     * Sends the given message to the current client using SSE format. Cannot be
     * called if {@link #isConnected()} returns false.
     *
     * @param message
     *            The message to send
     */
    protected void sendMessage(String message) {
        assert isConnected() : "Cannot send message when not connected";

        synchronized (lock) {
            try {
                // SSE format: event type, id, and data fields
                writer.write("event: uidl\n");
                writer.write("id: " + (ui.getInternals().getServerSyncId() - 1)
                        + "\n");
                // Data can span multiple lines - each line needs "data: "
                // prefix
                String[] lines = message.split("\n", -1);
                for (String line : lines) {
                    writer.write("data: " + line + "\n");
                }
                writer.write("\n"); // Empty line ends the event
                writer.flush();

                if (writer.checkError()) {
                    getLogger()
                            .debug("Error detected while writing SSE message");
                    connectionLost();
                }
            } catch (Exception e) {
                getLogger().debug("Failed to send SSE message", e);
                connectionLost();
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

        if (isConnected()) {
            disconnect();
        }

        this.asyncContext = asyncContext;

        // Set up SSE response headers
        HttpServletResponse response = (HttpServletResponse) asyncContext
                .getResponse();
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        // Prevent caching
        response.setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        // Keep connection alive
        response.setHeader("Connection", "keep-alive");
        // Disable buffering for immediate delivery
        response.setHeader("X-Accel-Buffering", "no");

        this.writer = response.getWriter();

        State oldState = state;
        state = State.CONNECTED;

        // Send initial connection event
        sendConnectionEstablishedEvent();

        if (oldState == State.PUSH_PENDING
                || oldState == State.RESPONSE_PENDING) {
            // Sending a "response" message (async=false) also takes care of a
            // pending push, but not vice versa
            push(oldState == State.PUSH_PENDING);
        }
    }

    /**
     * Sends an event to the client indicating that the SSE connection has been
     * established.
     */
    private void sendConnectionEstablishedEvent() {
        try {
            writer.write("event: connected\n");
            writer.write("data: {\"uiId\":" + ui.getUIId() + "}\n\n");
            writer.flush();
        } catch (Exception e) {
            getLogger().debug("Failed to send connection established event", e);
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
        // If a disconnection is already happening on another thread it is safe
        // to skip the operation
        if (disconnecting) {
            getLogger().debug(
                    "Disconnection already in progress, ignoring request");
            return;
        }

        synchronized (lock) {
            if (!isConnected() || asyncContext == null) {
                getLogger().debug(
                        "Disconnection already happened, ignoring request");
                return;
            }
            try {
                disconnecting = true;

                // Complete the async context to close the connection
                try {
                    asyncContext.complete();
                } catch (Exception e) {
                    getLogger().debug("Error when completing async context", e);
                }
                connectionLost();
            } finally {
                disconnecting = false;
            }
        }
    }

    /**
     * Called when the connection to the client has been lost.
     */
    public void connectionLost() {
        asyncContext = null;
        writer = null;
        if (state == State.CONNECTED) {
            state = State.DISCONNECTED;
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
        disconnecting = false;
        lock = new Object();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SsePushConnection.class.getName());
    }
}
