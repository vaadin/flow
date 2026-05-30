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

import com.google.gwt.xhr.client.XMLHttpRequest;

import elemental.json.JsonObject;

/**
 * Default implementation of the connection state handler. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/DefaultConnectionStateHandler.ts}.
 *
 * <p>
 * Construction takes a {@link DefaultConnectionStateHandlerCallbacks} adapter
 * so the TS class does not need to reach back through the Java {@code Registry}
 * facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "DefaultConnectionStateHandler")
public class DefaultConnectionStateHandler implements ConnectionStateHandler {

    public DefaultConnectionStateHandler(
            DefaultConnectionStateHandlerCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    @Override
    public native void heartbeatException(XMLHttpRequest request,
            String message);

    @Override
    public native void heartbeatInvalidStatusCode(XMLHttpRequest xhr);

    @Override
    public native void heartbeatOk();

    @Override
    public native void pushClosed(PushConnection pushConnection,
            Object responseObject);

    @Override
    public native void pushClientTimeout(PushConnection pushConnection,
            Object response);

    @Override
    public native void pushError(PushConnection pushConnection,
            Object response);

    @Override
    public native void pushReconnectPending(PushConnection pushConnection);

    @Override
    public native void pushOk(PushConnection pushConnection);

    @Override
    public native void pushScriptLoadError(String resourceUrl);

    @Override
    public native void xhrException(XhrConnectionError xhrConnectionError);

    @Override
    public native void xhrInvalidContent(XhrConnectionError xhrConnectionError);

    @Override
    public native void xhrInvalidStatusCode(
            XhrConnectionError xhrConnectionError);

    @Override
    public native void xhrOk();

    @Override
    public native void pushNotConnected(JsonObject payload);

    @Override
    public native void pushInvalidContent(PushConnection pushConnection,
            String message);

    @Override
    public native void configurationUpdated();
}
