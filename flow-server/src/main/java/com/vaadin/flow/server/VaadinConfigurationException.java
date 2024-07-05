/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

/**
 * Exception thrown for failures in the generation of a deployment configuration
 * object.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class VaadinConfigurationException extends Exception {

    /**
     * Exception constructor.
     *
     * @param message
     *            exception message
     * @param exception
     *            exception cause
     */
    public VaadinConfigurationException(String message, Exception exception) {
        super(message, exception);
    }
}
