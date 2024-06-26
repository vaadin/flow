/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.migration;

/**
 * @author Vaadin Ltd
 * @since 2.0
 */
public class MigrationFailureException extends Exception {

    public MigrationFailureException(String message) {
        super(message);
    }
}
