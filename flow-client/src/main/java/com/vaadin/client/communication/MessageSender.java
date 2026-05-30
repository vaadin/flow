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

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import elemental.json.JsonObject;

/**
 * Sends messages to the server. Internally uses XHR and/or push, depending on
 * the application configuration. Pure {@code @JsType(isNative=true)} binding to
 * the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/MessageSender.ts}.
 *
 * <p>
 * Construction takes a {@link MessageSenderCallbacks} adapter so the TS class
 * does not need to reach back through the Java {@code Registry} facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "MessageSender")
public class MessageSender {

    /** Resynchronization state mirrored from the TS string union. */
    public enum ResynchronizationState {
        NOT_ACTIVE, SEND_TO_SERVER, WAITING_FOR_RESPONSE
    }

    public MessageSender(MessageSenderCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    /** Sends an asynchronous UIDL request to the server. */
    public native void send(JsonObject payload);

    /** Sends any pending invocations to the server. */
    public native void sendInvocationsToServer();

    /**
     * Sends a beacon request indicating that the page is being unloaded.
     */
    public native void sendUnloadBeacon();

    /**
     * Sets the push connection enabled state. The {@code reEnableIfNeeded}
     * variant is exposed via {@link #setPushEnabled(boolean, boolean)}.
     */
    public native void setPushEnabled(boolean enabled);

    /**
     * Sets the push connection enabled state, controlling whether push should
     * be re-enabled if the configuration changes during disconnection.
     */
    public native void setPushEnabled(boolean enabled,
            boolean reEnableIfNeeded);

    /** Returns a human-readable description of the transport(s) being used. */
    public native String getCommunicationMethodName();

    /** Resynchronizes the client side. */
    public native void resynchronize();

    /**
     * Updates what id the server expects for the next client-to-server message.
     */
    public native void setClientToServerMessageId(int nextExpectedId,
            boolean force);

    /**
     * Modifies the resynchronize state to indicate that resync is desired.
     */
    public native boolean requestResynchronize();

    /** Clears the resynchronization state. */
    public native void clearResynchronizationState();

    /** Returns the current resynchronization state. */
    @JsOverlay
    public final ResynchronizationState getResynchronizationState() {
        return ResynchronizationState.valueOf(getResynchronizationStateName());
    }

    private native String getResynchronizationStateName();

    /** Returns whether the message queue still holds messages. */
    public native boolean hasQueuedMessages();

    /** Sends a beacon request. */
    public static native void sendBeacon(String url, String payload);
}
