/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import com.vaadin.flow.server.ExecutionFailedException;

/**
 * A generic command which may fail.
 * <p>
 * Note that this interface is not serializable and should not be used in a web
 * application.
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
