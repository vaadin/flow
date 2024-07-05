/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * A generic command which may fail.
 *
 * @author Vaadin Ltd
 * @since 2.0
 * @deprecated this command is an internal command and is not supposed to be
 *             used in application code
 */
@Deprecated
public interface FallibleCommand extends Serializable {

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
