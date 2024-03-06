/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import com.vaadin.flow.server.ExecutionFailedException;

/**
 * A generic command which may fail.
 * <p>
 * Note that this interface is not serializable and should not be used in a web
 * application.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public interface FallibleCommand {

    /**
     * Runs the given command.
     * <p>
     * If execution fails then the command may throw an exception which may give
     * a message and a cause of the failure.
     *
     * @throws ExecutionFailedException
     *             if there is an execution error
     */
    void execute() throws ExecutionFailedException;
}
