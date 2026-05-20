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
 * Detail about an error which occurred during an XHR request to the server.
 * Pure {@code @JsType(isNative=true)} binding to the TypeScript implementation
 * at
 * {@code src/main/frontend/internal/client/communication/XhrConnectionError.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "XhrConnectionError")
public class XhrConnectionError {

    public XhrConnectionError(XMLHttpRequest xhr, JsonObject payload,
            Exception exception) {
        // Defined by the TS class constructor.
    }

    /**
     * Returns the exception which caused the problem, if available.
     */
    public native Exception getException();

    /**
     * Returns the {@link XMLHttpRequest} which failed to reach the server.
     */
    public native XMLHttpRequest getXhr();

    /**
     * Returns the payload which was sent to the server.
     */
    public native JsonObject getPayload();
}
