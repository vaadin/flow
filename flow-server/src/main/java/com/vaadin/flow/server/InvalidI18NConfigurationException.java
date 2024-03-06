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
 * Exception indicating that the application's I18N localization has been
 * configured incorrectly.
 *
 * @since 1.0
 */
public class InvalidI18NConfigurationException extends RuntimeException {

    /**
     * Constructs a new invalid I18N localization configuration runtime
     * exception with the specified detail message.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public InvalidI18NConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid I18N localization configuration runtime
     * exception with the specified detail message.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <code>null</code> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public InvalidI18NConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
