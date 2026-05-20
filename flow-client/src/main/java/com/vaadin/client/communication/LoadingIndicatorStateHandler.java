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

import com.vaadin.client.JsBooleanSupplier;

/**
 * Manages the state of loading indicator based on active RPC requests, event
 * types, and lifecycle events. Pure {@code @JsType(isNative=true)} binding to
 * the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/LoadingIndicatorStateHandler.ts}.
 *
 * <p>
 * Construction takes a boolean supplier that returns
 * {@link RequestResponseTracker#hasActiveRequest()}, rather than the full
 * {@code Registry}, so the TS class does not depend on the Java facade.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "LoadingIndicatorStateHandler")
public class LoadingIndicatorStateHandler {

    public LoadingIndicatorStateHandler(JsBooleanSupplier hasActiveRequest) {
        // Defined by the TS class constructor.
    }

    /**
     * Updates the connection state to loading when a non-silent request starts.
     */
    public native void startLoading();

    /**
     * Updates the connection state to connected when active requests finish.
     */
    public native void stopLoading();

    /**
     * Processes an RPC message to determine if a loading indicator should be
     * displayed.
     *
     * @param rpcType
     *            the type of RPC request being processed
     * @param eventType
     *            for event RPC requests, the name of the event, otherwise
     *            {@code null}
     */
    public native void processMessage(String rpcType, String eventType);
}
