/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * An error thrown by the framework and handled by an {@link ErrorHandler}.
 * Typically handled by {@link VaadinSession#getErrorHandler()}.
 *
 * @since 1.0
 */
public class ErrorEvent implements Serializable {

    private final Throwable throwable;

    /**
     * Creates an error event which wraps the given throwable.
     *
     * @param throwable
     *            the throwable to wrap
     */
    public ErrorEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * Gets the contained throwable, the cause of the error.
     *
     * @return the throwable that caused the error
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Finds the error handler for the given session.
     *
     * @param session
     *            the active session
     * @return An ErrorHandler for the session or null if none was found
     */
    public static ErrorHandler findErrorHandler(VaadinSession session) {
        if (session == null) {
            return null;
        }
        return session.getErrorHandler();
    }

}
