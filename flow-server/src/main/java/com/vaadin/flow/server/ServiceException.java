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
 * Thrown for problems which occur in the {@link VaadinService} layer.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServiceException extends Exception {

    /**
     * Creates an exception which wraps the given throwable.
     *
     * @param throwable
     *            the throwable to wrap
     */
    public ServiceException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Creates an exception which contains the given message.
     *
     * @param message
     *            the message
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Creates an exception which wraps the given throwable and contains the
     * given message.
     *
     * @param message
     *            the message
     * @param throwable
     *            the throwable to wrap
     */
    public ServiceException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
