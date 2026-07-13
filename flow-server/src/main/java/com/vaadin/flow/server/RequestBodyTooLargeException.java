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
package com.vaadin.flow.server;

import java.io.IOException;

/**
 * Thrown when a client-to-server UIDL/RPC or push request body exceeds the
 * configured maximum size while it is being read.
 * <p>
 * Extends {@link IOException} so that it propagates through the existing
 * request-handling signatures. Request handlers translate this into an HTTP 413
 * (Request Entity Too Large) response, or, for push messages, into a refresh
 * and disconnect.
 *
 * @see com.vaadin.flow.function.DeploymentConfiguration#getMaxRequestBodySize()
 * @see SynchronizedRequestHandler#getRequestBody(java.io.Reader, long)
 * @since 25.3
 */
public class RequestBodyTooLargeException extends IOException {

    private final long maxBodySize;

    /**
     * Creates a new exception for a request body that exceeded the given
     * maximum size.
     *
     * @param maxBodySize
     *            the configured maximum request body size, in characters
     */
    public RequestBodyTooLargeException(long maxBodySize) {
        super("Request body exceeds the maximum allowed size of " + maxBodySize
                + " characters. The limit can be changed with the '"
                + InitParameters.SERVLET_PARAMETER_MAX_REQUEST_BODY_SIZE
                + "' configuration property (-1 disables it).");
        this.maxBodySize = maxBodySize;
    }

    /**
     * Gets the configured maximum request body size, in characters, that was
     * exceeded.
     *
     * @return the maximum request body size in characters
     */
    public long getMaxBodySize() {
        return maxBodySize;
    }
}
