/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.client.communication;

import com.vaadin.client.Command;

import elemental.json.JsonObject;

/**
 * Represents the client-side endpoint of a bidirectional ("push") communication
 * channel. Can be used to send UIDL request messages to the server and to
 * receive UIDL messages from the server (either asynchronously or as a response
 * to a UIDL request).
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface PushConnection {

    /**
     * Pushes a message to the server. Will throw an exception if the connection
     * is not active (see {@link #isActive()}).
     * <p>
     * Implementation detail: If the push connection is not connected and the
     * message can thus not be sent, the implementation must call
     * {@link ConnectionStateHandler#pushNotConnected(JsonObject)}, which will
     * retry the send later.
     * <p>
     * This method must not be called if the push connection is not
     * bidirectional (if {@link #isBidirectional()} returns false)
     *
     * @param payload
     *            the payload to push
     * @throws IllegalStateException
     *             if this connection is not active
     *
     * @see #isActive()
     */
    void push(JsonObject payload);

    /**
     * Checks whether this push connection is in a state where it can push
     * messages to the server. A connection is active until
     * {@link #disconnect(Command)} has been called.
     *
     * @return <code>true</code> if this connection can accept new messages;
     *         <code>false</code> if this connection is disconnected or
     *         disconnecting.
     */
    boolean isActive();

    /**
     * Closes the push connection. To ensure correct message delivery order, new
     * messages should not be sent using any other channel until it has been
     * confirmed that all messages pending for this connection have been
     * delivered. The provided command callback is invoked when messages can be
     * passed using some other communication channel.
     * <p>
     * After this method has been called, {@link #isActive()} returns
     * <code>false</code>. Calling this method for a connection that is no
     * longer active will throw an exception.
     *
     * @param command
     *            command to invoke when the connection has been properly
     *            disconnected
     * @throws IllegalStateException
     *             if this connection is not active
     */
    void disconnect(Command command);

    /**
     * Returns a human readable string representation of the transport type used
     * to communicate with the server.
     *
     * @return A human readable string representation of the transport type
     */
    String getTransportType();

    /**
     * Checks whether this push connection should be used for communication in
     * both directions or if an XHR should be used for client to server
     * communication.
     *
     * A bidirectional push connection must be able to reliably inform about its
     * connection state.
     *
     * @return true if the push connection should be used for messages in both
     *         directions, false if it should only be used for server to client
     *         messages
     */
    boolean isBidirectional();

}
