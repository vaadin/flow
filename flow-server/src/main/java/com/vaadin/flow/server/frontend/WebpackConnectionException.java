/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

/**
 * Exception thrown when webpack server doesn't respond with HTTP_OK for a
 * request.
 *
 * This exception usually means that webpack-dev-server failed compilation of
 * the frontend bundle and any error in the output should be fixed.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class WebpackConnectionException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message
     *            the detailed message on the problem.
     */
    public WebpackConnectionException(String message) {
        super(message);
    }
}
