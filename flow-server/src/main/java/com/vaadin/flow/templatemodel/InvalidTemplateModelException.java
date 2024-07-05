/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

/**
 * Exception thrown when encountering an invalid type in a template model.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class InvalidTemplateModelException extends RuntimeException {
    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message
     *            the exception message
     * @param cause
     *            the cause of the exception
     */
    public InvalidTemplateModelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with the given message.
     *
     * @param message
     *            the exception message
     */
    public InvalidTemplateModelException(String message) {
        super(message);
    }

}
