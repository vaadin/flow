/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

/**
 * A generic command interface meant to be used for passing lambdas around.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface Command {
    /**
     * Runs the given command.
     */
    void execute();
}
