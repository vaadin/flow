/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

/**
 * Exception for when there is a problem with compressing files.
 *
 * @author Vaadin Ltd
 * @since 24.3
 */
public class CompressionException extends RuntimeException {

    /**
     * Constructs a new compression exception with the specified detail message.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public CompressionException(String message) {
        super(message);
    }

    /**
     * Constructs a new compression exception with the specified detail message
     * and the root cause throwable for the exception.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     * @param cause
     *            throwable cause for the exception
     */
    public CompressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
