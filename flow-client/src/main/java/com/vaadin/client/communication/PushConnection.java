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
package com.vaadin.client.communication;

import jsinterop.annotations.JsType;

import com.vaadin.client.Command;

import elemental.json.JsonObject;

/**
 * Represents the client-side endpoint of a bidirectional ("push") communication
 * channel. Can be used to send UIDL request messages to the server and to
 * receive UIDL messages from the server (either asynchronously or as a response
 * to a UIDL request).
 *
 * <p>
 * Declared as a native {@code @JsType} interface so the TS-side
 * {@link AtmospherePushConnection} (also a native {@code @JsType}) can declare
 * it as an implemented interface; the actual implementation lives in
 * {@code src/main/frontend/internal/client/communication/PushConnection.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "PushConnection")
public interface PushConnection {

    /**
     * Pushes a message to the server. Will throw an exception if the connection
     * is not active (see {@link #isActive()}).
     *
     * <p>
     * Implementation detail: if the push connection is not connected, the
     * implementation must call
     * {@link ConnectionStateHandler#pushNotConnected(JsonObject)} so the
     * message can be retried later.
     *
     * <p>
     * This method must not be called if the push connection is not
     * bidirectional ({@link #isBidirectional()} returns false).
     */
    void push(JsonObject payload);

    /**
     * Checks whether this push connection is in a state where it can push
     * messages to the server. Active until {@link #disconnect(Command)} is
     * called.
     */
    boolean isActive();

    /**
     * Closes the push connection. Invokes the {@code command} once messages can
     * safely be sent through some other communication channel.
     */
    void disconnect(Command command);

    /**
     * Returns a human readable string representation of the transport type used
     * to communicate with the server.
     */
    String getTransportType();

    /**
     * Checks whether this push connection should be used for communication in
     * both directions or if XHR should be used for client-to-server messages.
     */
    boolean isBidirectional();
}
