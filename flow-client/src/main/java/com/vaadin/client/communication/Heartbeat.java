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

import com.vaadin.client.UILifecycle;

/**
 * Handles sending of heartbeats to the server and reacting to the response.
 * Pure {@code @JsType(isNative=true)} binding to the TypeScript implementation
 * at {@code src/main/frontend/internal/client/communication/Heartbeat.ts}.
 *
 * <p>
 * Construction takes the resolved heartbeat URI and a
 * {@link HeartbeatCallbacks} adapter that dispatches into
 * {@link ConnectionStateHandler}, so the TS class does not need to reach back
 * through the Java Registry facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "Heartbeat")
public class Heartbeat {

    public Heartbeat(String uri, int heartbeatInterval, UILifecycle uiLifecycle,
            HeartbeatCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    /** Sends a heartbeat to the server. */
    public native void send();

    /** Gets the heartbeat interval, in seconds. */
    public native int getInterval();

    /** Updates the schedule to match the set interval. */
    public native void schedule();

    /**
     * Changes the heartbeat interval at runtime and applies it.
     *
     * @param heartbeatInterval
     *            new interval in seconds
     */
    public native void setInterval(int heartbeatInterval);
}
