/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

/**
 * Exception indicating that something went wrong while resolving navigation
 * routes.
 *
 * @since 1.0
 */
public class NotFoundException extends RuntimeException {

    /**
     * Default constructor.
     */
    public NotFoundException() {
    }

    /**
     * Navigation exception thrown when routing fails due to a faulty navigation
     * target string.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public NotFoundException(String message) {
        super(message);
    }
}
