/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

/**
 * Upload exception class.
 *
 * @since 1.0
 */
public class UploadException extends Exception {
    /**
     * Exception constructor with exception.
     *
     * @param exception
     *            exception
     */
    public UploadException(Exception exception) {
        super("Upload failed", exception);
    }

    /**
     * Exception constructor with message.
     *
     * @param message
     *            error message
     */
    public UploadException(String message) {
        super(message);
    }
}
