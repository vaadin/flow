/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.installer;

/**
 * Exception indicating a failure during downloaded archive verification.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public final class VerificationException extends Exception {

    /**
     * Exceptioon with message.
     *
     * @param message
     *            exception message
     */
    public VerificationException(String message) {
        super(message);
    }

    /**
     * Exceptioon with message and cause.
     *
     * @param message
     *            exception message
     * @param cause
     *            cause for exception
     */
    VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
