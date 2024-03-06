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
 * Defines the interface to handle exceptions thrown during the execution of a
 * FutureAccess.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ErrorHandlingCommand extends Command {

    /**
     * Handles exceptions thrown during the execution of a FutureAccess.
     *
     * @param exception
     *            the thrown exception.
     */
    void handleError(Exception exception);

}
