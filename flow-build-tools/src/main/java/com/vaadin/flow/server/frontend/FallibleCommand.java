/*
 * Copyright 2000-2025 Vaadin Ltd.
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

/**
 * A generic command which may fail.
 * <p>
 * Note that this interface is not serializable and should not be used in a web
 * application.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
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

    /**
     * Accepts {@link GeneratedFilesSupport} utility allows to track generated
     * files but write them only when the content is changed, preventing
     * filesystem watchers to be triggered when not required.
     *
     * @param support
     *            the generated file support utility to use.
     */
    default void setGeneratedFileSupport(GeneratedFilesSupport support) {
    }

}
