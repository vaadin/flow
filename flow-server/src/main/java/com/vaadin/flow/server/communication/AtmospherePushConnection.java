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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.BroadcastFilterAdapter;
import org.atmosphere.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.shared.communication.PushConstants;

/**
 * A {@link PushConnection} implementation using the Atmosphere push support
 * that is by default included in Vaadin.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AtmospherePushConnection
        implements PushConnection, FragmentedMessageHolder {

    private UI ui;
    private transient State state = State.DISCONNECTED;
    private transient AtmosphereResource resource;
    private transient FragmentedMessage incomingMessage;
    private transient Future<Object> outgoingMessage;
    private transient Object lock = new Object();
    private volatile boolean disconnecting;

    /**
     * Represents a message that can arrive as multiple fragments.
     */
    public static class FragmentedMessage implements Serializable {

        private final StringBuilder message = new StringBuilder();
        private int messageLength = -1;

        /**
         * Creates a message by reading from the given reader.
         * <p>
         * Immediately reads the length of the message (up until
         * {@value PushConstants#MESSAGE_DELIMITER}) from the reader.
         */
        public FragmentedMessage() {
        }

        private void readMessageLength(Reader reader) throws IOException {
            // Messages are prefixed by the total message length plus a
            // delimiter
            String length = "";
            int c;
            while ((c = reader.read()) != -1
                    && c != PushConstants.MESSAGE_DELIMITER) {
                length += (char) c;
            }
            try {
                messageLength = Integer.parseInt(length);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid message length " + length, e);
            }
        }

        /**
         * Appends all the data from the given Reader to this message and
         * returns whether the message was completed.
         *
         * @param reader
         *            The Reader from which to read.
         * @return true if this message is complete, false otherwise.
         * @throws IOException
         *             if an IO error occurred
         */
        public boolean append(Reader reader) throws IOException {
            if (messageLength == -1) {
                readMessageLength(reader);
            }

            char[] buffer = new char[PushConstants.WEBSOCKET_BUFFER_SIZE];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                message.append(buffer, 0, read);
                assert message.length() <= messageLength : "Received message "
                        + message.length() + "chars, expected " + messageLength;
            }
            return message.length() == messageLength;
        }

        public Reader getReader() {
            return new StringReader(message.toString());
        }
    }

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
        CONNECTED;
    }

    /**
     * Creates an instance connected to the given UI.
     *
     * @param ui
     *            the UI to which this connection belongs
     */
    public AtmospherePushConnection(UI ui) {
        this.ui = ui;

        UsageStatistics.markAsUsed("flow/AtmospherePushConnection",
                getAtmosphereVersion());
    }

    /**
     * Gets the Atmosphere version in use, as reported by
     * {@link Version#getRawVersion()}.
     *
     * @return the Atmosphere version in use or null if Atmosphere was not found
     */
    public static String getAtmosphereVersion() {
        try {
            String v = Version.getRawVersion();
            assert v != null;
            return v;
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    @Override
    public void push() {
        push(true);
    }

    /**
     * Pushes pending state changes and client RPC calls to the client. If
     * {@code isConnected()} is false, defers the push until a connection is
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
     * Sends the given message to the current client. Cannot be called if
     * {@link #isConnected()} returns false.
     *
     * @param message
     *            The message to send
     */
    protected void sendMessage(String message) {
        assert (isConnected());
        // "Broadcast" the changes to the single client only
        outgoingMessage = getResource().getBroadcaster().broadcast(
                new PushMessage(ui.getInternals().getServerSyncId() - 1,
                        message),
                getResource());
    }

    /**
     * Reads and buffers a (possibly partial) message. If a complete message was
     * received, or if the call resulted in the completion of a partially
     * received message, returns a {@link Reader} yielding the complete message.
     * Otherwise, returns null.
     *
     * @param resource
     *            The atmosphere resource with data
     * @param reader
     *            The request body reader
     * @param holder
     *            A holder for a previously received partial message
     * @return a Reader yielding the complete message, or {@code null} if the
     *         received message was a partial message
     * @throws IOException
     *             if an IO error occurred
     */
    protected static Reader receiveMessage(AtmosphereResource resource,
            Reader reader, FragmentedMessageHolder holder) throws IOException {

        if (resource == null || resource.transport() != TRANSPORT.WEBSOCKET) {
            return reader;
        }

        FragmentedMessage msg = holder.getOrCreateFragmentedMessage(resource);
        if (msg.append(reader)) {
            Reader messageReader = msg.getReader();
            holder.clearFragmentedMessage(resource);
            return messageReader;
        } else {
            // Only received a partial message
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        assert state != null;
        assert (state == State.CONNECTED) ^ (resource == null);
        return state == State.CONNECTED;
    }

    /**
     * Associates this {@code AtmospherePushConnection} with the given
     * {@link AtmosphereResource} representing an established push connection.
     * If already connected, calls {@link #disconnect()} first. If there is a
     * deferred push, carries it out via the new connection.
     *
     * @param resource
     *            the resource to associate this connection with
     */
    public void connect(AtmosphereResource resource) {

        assert resource != null;
        assert resource != this.resource;

        if (isConnected()) {
            disconnect();
        }

        this.resource = resource;
        State oldState = state;
        state = State.CONNECTED;

        if (oldState == State.PUSH_PENDING
                || oldState == State.RESPONSE_PENDING) {
            // Sending a "response" message (async=false) also takes care of a
            // pending push, but not vice versa
            push(oldState == State.PUSH_PENDING);
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
     * Gets the AtmosphereResource associated with this connection.
     *
     * @return The AtmosphereResource associated with this connection or null if
     *         connection not open.
     */
    protected AtmosphereResource getResource() {
        return resource;
    }

    @Override
    public void disconnect() {
        // If a disconnection is already happening on another thread it is safe
        // to skip the operation. This also prevents potential deadlocks if the
        // container acquires locks during operations on HTTP session, as
        // closing the AtmosphereResource may cause HTTP session access
        if (disconnecting) {
            getLogger().debug(
                    "Disconnection already in progress, ignoring request");
            return;
        }

        synchronized (lock) {
            if (!isConnected() || resource == null) {
                // Already disconnected. Should not happen but if it does,
                // we don't want to cause NPEs
                getLogger().debug(
                        "Disconnection already happened, ignoring request");
                return;
            }
            try {
                disconnecting = true;
                if (resource.isResumed()) {
                    // This can happen for long polling because of
                    // http://dev.vaadin.com/ticket/16919
                    // Once that is fixed, this should never happen
                    connectionLost();
                    return;
                }
                if (outgoingMessage != null) {
                    // Wait for the last message to be sent before closing the
                    // connection (assumes that futures are completed in order)
                    try {
                        outgoingMessage.get(1000, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        if (ui.isClosing()) {
                            getLogger().debug(
                                    "Something was not sent to client on an UI that was already closed by beacon request or similar. This seems to happen with Safari occassionally when navigating away from a UI.");
                        } else {
                            getLogger().info(
                                    "Timeout waiting for messages to be sent to client before disconnect",
                                    e);
                        }
                    } catch (Exception e) {
                        getLogger().info(
                                "Error waiting for messages to be sent to client before disconnect",
                                e);
                    }
                    outgoingMessage = null;
                }
                try {
                    resource.close();
                } catch (IOException e) {
                    getLogger().info("Error when closing push connection", e);
                }
                connectionLost();
            } finally {
                disconnecting = false;
            }
        }
    }

    /**
     * Called when the connection to the client has been lost.
     *
     */
    public void connectionLost() {
        resource = null;
        if (state == State.CONNECTED) {
            // Guard against connectionLost being (incorrectly) called when
            // state is PUSH_PENDING or RESPONSE_PENDING
            // (http://dev.vaadin.com/ticket/16919)
            state = State.DISCONNECTED;
        }

    }

    @Override
    public FragmentedMessage getOrCreateFragmentedMessage(
            AtmosphereResource resource) {
        if (incomingMessage == null) {
            incomingMessage = new FragmentedMessage();
        }
        return incomingMessage;
    }

    @Override
    public void clearFragmentedMessage(AtmosphereResource resource) {
        incomingMessage = null;
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
        return LoggerFactory
                .getLogger(AtmospherePushConnection.class.getName());
    }

    /**
     * Internal method used for reconfiguring loggers to show all Atmosphere log
     * messages in the console.
     *
     */
    public static void enableAtmosphereDebugLogging() {
        getLogger().warn(
                "Enable logging of 'org.atmosphere' through your slf4j implementation"
                        + " instead (i.e.: logback, log4j, etc)");
    }

    static final class PushMessage implements Serializable {
        final int serverSyncId;
        final String message;

        PushMessage(int serverSyncId, String message) {
            this.serverSyncId = serverSyncId;
            this.message = message;
        }

        boolean alreadySeen(int lastSeenOnClient) {
            return serverSyncId <= lastSeenOnClient;
        }

        @Override
        public String toString() {
            return "PushMessage " + serverSyncId + ", body: " + message;
        }
    }

    /**
     * A {@link org.atmosphere.cpr.BroadcastFilter} that unwraps the message to
     * be sent to the client from a {@link PushMessage} instance.
     */
    static final class PushMessageUnwrapFilter extends BroadcastFilterAdapter
            implements Serializable {
        @Override
        public BroadcastAction filter(String broadcasterId,
                AtmosphereResource r, Object originalMessage, Object message) {
            if (message instanceof AtmospherePushConnection.PushMessage) {
                message = ((PushMessage) message).message;
            }
            return new BroadcastAction(message);
        }
    }
}
