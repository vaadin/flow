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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;

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
            Constants.PACKAGE_LOCK_YAML, Constants.PACKAGE_LOCK_BUN,
            Constants.PACKAGE_LOCK_BUN_1_2, TaskGenerateTsConfig.TSCONFIG_JSON,
            TaskGenerateTsDefinitions.TS_DEFINITIONS, ".pnpmfile.cjs", ".npmrc",
            FrontendUtils.VITE_GENERATED_CONFIG, FrontendUtils.VITE_CONFIG);
    private Set<File> existingFiles = new HashSet<>();

    private List<String> hillaGenerated = List.of("file-routes.ts",
            "file-routes.json");

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param options
     *            options containing file paths and classfinder
     */
    public TaskCleanFrontendFiles(Options options) {
        this.projectRoot = options.getNpmFolder();

        Arrays.stream(projectRoot
                .listFiles(file -> generatedFiles.contains(file.getName())))
                .forEach(existingFiles::add);

        // If we have an existing package.json or run Hilla, do not remove
        // node_modules
        boolean hillaUsed = FrontendUtils.isHillaUsed(
                options.getFrontendDirectory(), options.getClassFinder());

        if (existingFiles.contains(
                new File(projectRoot, Constants.PACKAGE_JSON)) || hillaUsed) {
            existingFiles.add(new File(projectRoot, NODE_MODULES));
        }
        // If hilla is not used clean generated hilla files.
        if (!hillaUsed) {
            hillaGenerated.forEach(
                    file -> new File(options.getFrontendGeneratedFolder(), file)
                            .delete());
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
