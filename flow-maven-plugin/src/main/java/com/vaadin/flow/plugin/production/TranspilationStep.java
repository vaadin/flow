/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.plugin.common.FrontendToolsManager;

/**
 * Transpiles artifacts in the specified directory.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class TranspilationStep {
    private final FrontendToolsManager frontendToolsManager;

    /**
     * Prepares the step.
     *
     * @param frontendToolsManager
     *            the manager to be used to transpile files, not {@code null}
     * @param networkConcurrency
     *            maximum number of concurrent network requests
     */
    public TranspilationStep(FrontendToolsManager frontendToolsManager,
            int networkConcurrency) {
        this.frontendToolsManager = Objects
                .requireNonNull(frontendToolsManager);
        frontendToolsManager.installFrontendTools(networkConcurrency);
    }

    /**
     * Transpiles the files from source directory into the output directory.
     *
     * @param es6SourceDirectory
     *            the directory with original ES6 files
     * @param outputDirectory
     *            the directory that will have processed files in
     * @param skipEs5
     *            whether to skip the transpilation step or not
     * @throws IllegalStateException
     *             if no transpilation results found
     * @throws UncheckedIOException
     *             if {@link IOException} occurs during file operations
     */
    public void transpileFiles(File es6SourceDirectory, File outputDirectory,
            boolean skipEs5) {
        Map<String, File> transpilationResult = frontendToolsManager
                .transpileFiles(es6SourceDirectory, outputDirectory, skipEs5);
        if (transpilationResult.isEmpty()) {
            throw new IllegalStateException(
                    "Received no transpilation results from frontend tools");
        }
        transpilationResult.values().stream().filter(
                configurationOutput -> !configurationOutput.isDirectory())
                .findAny().ifPresent(nonExistingDirectory -> {
                    throw new IllegalStateException(String.format(
                            "Transpilation output at '%s' is not a directory or does not exist",
                            nonExistingDirectory));
                });
    }
}
