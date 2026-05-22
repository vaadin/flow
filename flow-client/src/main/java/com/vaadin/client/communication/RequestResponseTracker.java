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

import com.google.web.bindery.event.shared.HandlerRegistration;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsIntConsumer;
import com.vaadin.client.JsRunnable;

/**
 * Tracks active server UIDL requests. Pure {@code @JsType(isNative=true)}
 * binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/RequestResponseTracker.ts}.
 *
 * <p>
 * The TS implementation replaces the GWT EventBus with simple per-event
 * callback queues; the four event types (RequestStarting/ResponseHandling
 * Started/Ended/ReconnectionAttempt) are gone, replaced by direct
 * {@code JsRunnable} and {@code JsIntConsumer} callbacks.
 *
 * <p>
 * Construction takes a {@link JsRunnable} that wraps the original endRequest
 * "should we flush queued invocations now?" decision; the TS class doesn't need
 * to dispatch back through the still-Java MessageSender / ServerRpcQueue
 * facades to make that call.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "RequestResponseTracker")
public class RequestResponseTracker {

    public RequestResponseTracker(JsRunnable maybeFlushInvocations) {
        // Defined by the TS class constructor.
    }

    /**
     * Marks that a new request has started. Fires the request-starting event.
     */
    public native void startRequest();

    /** @return whether there is an active UIDL request. */
    public native boolean hasActiveRequest();

    /**
     * Marks the current request as ended. Fires the response-handling-ended
     * event.
     */
    public native void endRequest();

    /** Fires a response-handling-started event. */
    public native void fireResponseHandlingStarted();

    /** Fires a reconnection-attempt event with the given attempt number. */
    public native void fireReconnectionAttempt(int attempt);

    /** Adds a handler for request-starting events. */
    public native HandlerRegistration addRequestStartingHandler(
            JsRunnable handler);

    /** Adds a handler for response-handling-started events. */
    public native HandlerRegistration addResponseHandlingStartedHandler(
            JsRunnable handler);

    /** Adds a handler for response-handling-ended events. */
    public native HandlerRegistration addResponseHandlingEndedHandler(
            JsRunnable handler);

    /**
     * Adds a handler for reconnection-attempt events; handler receives the
     * attempt number.
     */
    public native HandlerRegistration addReconnectionAttemptHandler(
            JsIntConsumer handler);
}
