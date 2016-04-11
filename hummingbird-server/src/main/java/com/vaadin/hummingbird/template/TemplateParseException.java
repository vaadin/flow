package com.vaadin.hummingbird.template;

/**
 * Exception thrown when there's a problem parsing a template.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TemplateParseException extends RuntimeException {
    /**
     * Creates a new template parse exception with the given message.
     *
     * @param message
     *            the exception message
     */
    public TemplateParseException(String message) {
        super(message);
    }
}
