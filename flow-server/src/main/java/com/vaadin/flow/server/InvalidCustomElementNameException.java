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
 * Exception indicating that the custom-element name is invalid.
 *
 * @since 1.0
 */
public class InvalidCustomElementNameException extends RuntimeException {

    /**
     * Constructs a new invalid custom element name runtime exception with the
     * specified detail message.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public InvalidCustomElementNameException(String message) {
        super(message);
    }
}
