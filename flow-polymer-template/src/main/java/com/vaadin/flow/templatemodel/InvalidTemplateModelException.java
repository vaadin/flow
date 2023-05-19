/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

/**
 * Exception thrown when encountering an invalid type in a template model.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a> For lit templates, you can use {@code @Id}
 *             mapping and the component API or the element API with property
 *             synchronization instead.
 */
@Deprecated
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
