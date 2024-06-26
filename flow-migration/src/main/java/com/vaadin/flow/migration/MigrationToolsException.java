/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.migration;

/**
 * Exception thrown for missing migration tools.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class MigrationToolsException extends Exception {

    /**
     * Migration tool exception constructor.
     *
     * @param message
     *            message on why this exception was thrown
     */
    public MigrationToolsException(String message) {
        super(message);
    }

}
