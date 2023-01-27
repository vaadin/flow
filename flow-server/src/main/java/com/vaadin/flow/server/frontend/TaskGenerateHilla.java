/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.io.File;

/**
 * Generate the Vaadin TS files for endpoints, and the Client API file. It uses
 * the new Maven/Gradle plugin based generator.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public interface TaskGenerateHilla extends FallibleCommand {
    /**
     * Configures the task by passing it some parameters.
     *
     * @param projectDirectory
     *            the project root directory. In a Maven multi-module project,
     *            this is the module root, not the main project one.
     * @param buildDirectoryName
     *            the name of the build directory (i.e. "build" or "target").
     */
    default void configure(File projectDirectory, String buildDirectoryName) {
    }
}
