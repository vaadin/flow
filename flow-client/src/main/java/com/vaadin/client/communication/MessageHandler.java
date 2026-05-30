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
import com.vaadin.client.ValueMap;
import com.vaadin.client.flow.collection.JsArray;

/**
 * Handles incoming UIDL messages from the server and ensures connectors are
 * updated accordingly. Pure {@code @JsType(isNative=true)} binding to the
 * TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/MessageHandler.ts}.
 *
 * <p>
 * Construction takes a {@link MessageHandlerCallbacks} adapter so the TS class
 * does not need to reach back through the Java {@code Registry} facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "MessageHandler")
public class MessageHandler {

    public MessageHandler(MessageHandlerCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    /** Handles a received UIDL JSON message. */
    public native void handleMessage(ValueMap json);

    /** Gets the server id included in the last received response. */
    public native int getLastSeenServerSyncId();

    /**
     * Returns the profiling data array exposed on the client as
     * {@code client.getProfilingData()}.
     */
    public native JsArray<Object> getProfilingData();

    /** Gets the CSRF token used by the application. */
    public native String getCsrfToken();

    /** Gets the push connection identifier for this session. */
    public native String getPushId();

    /** Checks if the first UIDL has been handled. */
    public native boolean isInitialUidlHandled();

    /** Postpones rendering of a response for a short period of time. */
    public native void suspendReponseHandling(Object lock);

    /** Resumes the rendering process once all locks have been removed. */
    public native void resumeResponseHandling(Object lock);

    /**
     * Sets a temporary handler for session expiration on the next response.
     */
    public native void setNextResponseSessionExpiredHandler(JsRunnable handler);

    /**
     * Parses the given JSON from the server.
     *
     * @param jsonText
     *            the json from the server
     * @return a parsed ValueMap or null if the input could not be parsed
     */
    public static native ValueMap parseJson(String jsonText);
}
