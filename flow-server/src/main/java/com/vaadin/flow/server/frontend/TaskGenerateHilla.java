/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
