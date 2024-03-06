/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

/**
 * Exception thrown if the UI has been detached when it should not be.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UIDetachedException extends RuntimeException {

    /**
     * Creates an instance of the exception.
     */
    public UIDetachedException() {
        super();
    }

    /**
     * Creates an instance of the exception using the given message and cause.
     *
     * @param message
     *            the message to use
     * @param cause
     *            the cause of the exception
     */
    public UIDetachedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an instance of the exception using the given message.
     *
     * @param message
     *            the message to use
     */
    public UIDetachedException(String message) {
        super(message);
    }

    /**
     * Creates an instance of the exception using the given cause.
     *
     * @param cause
     *            the cause of the exception
     */
    public UIDetachedException(Throwable cause) {
        super(cause);
    }

}
