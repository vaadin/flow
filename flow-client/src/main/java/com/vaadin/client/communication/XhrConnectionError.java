/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.google.gwt.xhr.client.XMLHttpRequest;

import elemental.json.JsonObject;

/**
 * XhrConnectionError provides detail about an error which occurred during an
 * XHR request to the server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class XhrConnectionError {

    private final Exception exception;
    private final XMLHttpRequest xhr;
    private final JsonObject payload;

    /**
     * Creates a XhrConnectionError for the given request using the given
     * payload.
     *
     * @param xhr
     *            the request which caused the error
     * @param payload
     *            the payload which was on its way to the server
     * @param exception
     *            the exception which caused the error or null if the error was
     *            not caused by an exception
     */
    public XhrConnectionError(XMLHttpRequest xhr, JsonObject payload,
            Exception exception) {
        this.xhr = xhr;
        this.payload = payload;
        this.exception = exception;
    }

    /**
     * Returns the exception which caused the problem, if available.
     *
     * @return the exception which caused the problem, or null if not available
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Returns {@link XMLHttpRequest} which failed to reach the server.
     *
     * @return the request which failed
     *
     */
    public XMLHttpRequest getXhr() {
        return xhr;
    }

    /**
     * Returns the payload which was sent to the server.
     *
     * @return the payload which was sent, never null
     */
    public JsonObject getPayload() {
        return payload;
    }
}
