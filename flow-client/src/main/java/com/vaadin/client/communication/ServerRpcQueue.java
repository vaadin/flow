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

import com.vaadin.client.JsRunnable;
import com.vaadin.client.UILifecycle;

import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Manages the queue of server invocations (RPC) which are waiting to be sent to
 * the server. Pure {@code @JsType(isNative=true)} binding to the TypeScript
 * implementation at
 * {@code src/main/frontend/internal/client/communication/ServerRpcQueue.ts}.
 *
 * <p>
 * Construction takes a {@link UILifecycle} (only used for an
 * {@code isRunning()} guard on {@link #add(JsonValue)}) and a deferred-flush
 * callback that dispatches into
 * {@link MessageSender#sendInvocationsToServer()}, so the TS class does not
 * depend on the Java {@code Registry}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "ServerRpcQueue")
public class ServerRpcQueue {

    public ServerRpcQueue(UILifecycle uiLifecycle, JsRunnable send) {
        // Defined by the TS class constructor.
    }

    /**
     * Adds an explicit RPC method invocation to the send queue.
     */
    public native void add(JsonValue invocation);

    /**
     * Clears the queue and any pending flush.
     */
    public native void clear();

    /**
     * Returns the current size of the queue.
     */
    public native int size();

    /**
     * Returns {@code true} when the queue has no pending invocations.
     */
    public native boolean isEmpty();

    /**
     * Triggers a deferred flush of the queued invocations.
     */
    public native void flush();

    /**
     * Returns {@code true} when a flush is currently pending.
     */
    public native boolean isFlushPending();

    /**
     * Returns whether the loading indicator should be shown while the
     * dispatched invocations are awaiting a server response.
     */
    public native boolean showLoadingIndicator();

    /**
     * Returns the queued invocations as the JSON-array payload to send.
     */
    public native JsonArray toJson();
}
