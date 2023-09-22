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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;

/**
 * Clean any frontend files generated for creation on a new development or
 * production bundle.
 * <p>
 * For a project containing {@code package.json} or is using Hilla,
 * {@code node_modules} will be retained.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskCleanFrontendFiles implements FallibleCommand {

    public static final String NODE_MODULES = "node_modules";

    private File projectRoot;

    private List<String> generatedFiles = List.of(NODE_MODULES,
            Constants.PACKAGE_JSON, Constants.PACKAGE_LOCK_JSON,
            Constants.PACKAGE_LOCK_YAML, TaskGenerateTsConfig.TSCONFIG_JSON,
            TaskGenerateTsDefinitions.TS_DEFINITIONS, ".pnpmfile.cjs", ".npmrc",
            FrontendUtils.VITE_GENERATED_CONFIG, FrontendUtils.VITE_CONFIG);
    private Set<File> existingFiles = new HashSet<>();

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param projectRoot
     *            project root folder
     */
    public TaskCleanFrontendFiles(File projectRoot) {
        this.projectRoot = projectRoot;

        Arrays.stream(projectRoot
                .listFiles(file -> generatedFiles.contains(file.getName())))
                .forEach(existingFiles::add);

        // If we have an existing package.json or run Hilla, do not remove
        // node_modules
        if (existingFiles
                .contains(new File(projectRoot, Constants.PACKAGE_JSON))
                || EndpointRequestUtil.isHillaAvailable()) {
            existingFiles.add(new File(projectRoot, NODE_MODULES));
        }
    }

    @Override
    public void execute() throws ExecutionFailedException {
        final List<File> filesToRemove = Arrays
                .stream(projectRoot.listFiles(
                        file -> generatedFiles.contains(file.getName())
                                && !existingFiles.contains(file)))
                .collect(Collectors.toList());
        for (File file : filesToRemove) {
            log().debug("Removing file {}", file);
            try {
                if (file.isDirectory()) {
                    FrontendUtils.deleteDirectory(file);
                } else {
                    file.delete();
                }
            } catch (IOException ioe) {
                log().warn("Could not delete file {} due to {}", file,
                        ioe.getMessage());
                log().debug("Failed to remove file", ioe);
            }
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
