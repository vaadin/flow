/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import com.vaadin.flow.server.frontend.FallibleCommand;

/**
 * Thrown by {@link FallibleCommand} if it's unable to complete its execution.
 *
 * @author Vaadin Ltd
 * @since 2.0
 *
 */
public class ExecutionFailedException extends Exception {

    /**
     * Creates a new exception instance.
     */
    public ExecutionFailedException() {
    }

    /**
     * Creates a new exception instance with the given {@code message}.
     *
     * @param message
     *            the exception message
     */
    public ExecutionFailedException(String message) {
        super(message);
    }

    /**
     * Creates a new exception instance with the given {@code cause}.
     *
     * @param cause
     *            the exception cause
     */
    public ExecutionFailedException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception instance with the given {@code message} and
     * {@code cause}.
     *
     * @param message
     *            the exception message
     * @param cause
     *            the exception cause
     */
    public ExecutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
