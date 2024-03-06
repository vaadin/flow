/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

/**
 * {@code UnsupportedPropertyTypeException} is throw when
 * {@link com.vaadin.flow.component.WebComponentExporter} tries to register a
 * property encasing an unsupported value type.
 *
 * @since 2.0
 */
public class UnsupportedPropertyTypeException extends RuntimeException {
    /**
     * Constructs a new runtime exception with the specified detail message. The
     * cause is not initialized, and may subsequently be initialized by a call
     * to {@link #initCause}.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public UnsupportedPropertyTypeException(String message) {
        super(message);
    }
}
