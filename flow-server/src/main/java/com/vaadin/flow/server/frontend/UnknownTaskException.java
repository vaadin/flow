/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

/**
 * Exception thrown for when a node task that is not in the task list is
 * encountered.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class UnknownTaskException extends RuntimeException {

    /**
     * Exception constructor.
     *
     * @param command
     *            command that was not found
     */
    public UnknownTaskException(FallibleCommand command) {
        super("Could not find position for task " + command.getClass());
    }
}
