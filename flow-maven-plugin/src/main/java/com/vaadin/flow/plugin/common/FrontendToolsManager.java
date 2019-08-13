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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.utils.FlowFileUtils;

/**
 * Entity to operate frontend tools to transpile files.
 * <p>
 * Transpilation is based on <a href="https://github.com/gulpjs/gulp">gulp</a>
 * and <a href="https://github.com/Polymer/polymer-build">polymer-build</a>.
 * <p>
 * Polymer tools are used since they are the only ones capable of bundling html
 * files along with css and js ones and providing helper method for processing
 * web components.
 * <p>
 * Apart from gulp and polymer-build,
 * <a href="https://github.com/yarnpkg/yarn">yarn</a> and
 * <a href="https://github.com/nodejs/node">node</a> are used to get and launch
 * the tools required.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class FrontendToolsManager {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FrontendToolsManager.class);

    private final RunnerManager runnerManager;
    private final File workingDirectory;
    private final String es5OutputDirectoryName;
    private final String es6OutputDirectoryName;
    private final FrontendDataProvider frontendDataProvider;

    /**
     * Prepares the manager.
     *
     * @param workingDirectory
     *            the directory to install and process files in, not
     *            {@code null}
     * @param es5OutputDirectoryName
     *            the name for the directory to put transpiled ES5 files into,
     *            not {@code null}
     * @param es6OutputDirectoryName
     *            the name for the directory to put minified ES6 files into, not
     *            {@code null}
     * @param frontendDataProvider
     *            the source of the files required by the
     *            {@link FrontendToolsManager}, not {@code null}
     * @param runnerManager
     *            node, gulp and yarn runner
     */
    public FrontendToolsManager(File workingDirectory,
            String es5OutputDirectoryName, String es6OutputDirectoryName,
            FrontendDataProvider frontendDataProvider,
            RunnerManager runnerManager) {
        FlowFileUtils
                .forceMkdir(Objects.requireNonNull(workingDirectory));
        this.runnerManager = Objects.requireNonNull(runnerManager);
        this.workingDirectory = workingDirectory;
        this.es5OutputDirectoryName = Objects
                .requireNonNull(es5OutputDirectoryName);
        this.es6OutputDirectoryName = Objects
                .requireNonNull(es6OutputDirectoryName);
        this.frontendDataProvider = Objects
                .requireNonNull(frontendDataProvider);
    }

    /**
     * Installs tools required for transpilation.
     * 
     * Tools installed are specified in the {@literal package.json} file in the
     * plugin project's resources directory. Additionally, copies numerous files
     * required for transpilation into the working directory.
     *
     * @param networkConcurrency
     *            maximum number of concurrent network requests
     *
     * @throws IllegalStateException
     *             if dependency installation fails
     * @throws UncheckedIOException
     *             if supplementary file creation fails
     */
    public void installFrontendTools(int networkConcurrency) {
        LOGGER.info("Installing required frontend tools to '{}'",
                workingDirectory);
        createFileFromTemplateResource("package.json", Collections.emptyMap());
        createFileFromTemplateResource("yarn.lock", Collections.emptyMap());
        try {

            StringBuilder args = new StringBuilder("install");
            if (networkConcurrency >= 0) {
                args.append(" --network-concurrency ");
                args.append(networkConcurrency);
            }
            runnerManager.getYarnRunner().execute(args.toString(),
                    Collections.emptyMap());
        } catch (TaskRunnerException e) {
            throw new IllegalStateException(
                    "Failed to install required frontend dependencies", e);
        }
    }

    private void createFileFromTemplateResource(String templateResourceName,
            Map<String, String> replacements) {
        try (InputStream resource = getClass().getClassLoader()
                .getResourceAsStream(templateResourceName)) {
            if (resource == null) {
                throw new IllegalStateException(String.format(
                        "File '%s' is not found in application resources",
                        templateResourceName));
            }

            String resourceContents = IOUtils.toString(resource,
                    StandardCharsets.UTF_8);
            for (Map.Entry<String, String> replacement : replacements
                    .entrySet()) {
                resourceContents = resourceContents.replaceFirst(
                        Pattern.quote(replacement.getKey()),
                        replacement.getValue().replace("\\", "/"));
            }

            FileUtils.write(new File(workingDirectory, templateResourceName),
                    resourceContents, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create a file in '%s'", workingDirectory), e);
        }
    }

    /**
     * Transpiles files in the directory provided, copying them to the working
     * directory. Creates a new output directory, in working directory, with two
     * separate directories for ES5 (transpiled) and ES6 (optimized) files.
     * Additionally, creates supplementary files, used by transpilation process.
     *
     * @param es6SourceDirectory
     *            the directory to get ES6 files from, not {@code null}
     * @param outputDirectory
     *            the directory to put transpiled files into, created if absent
     * @param skipEs5
     *            whether to skip the transpilation step or not
     * @return generated output directory path
     * @throws IllegalStateException
     *             if transpilation fails
     * @throws IllegalArgumentException
     *             if es6SourceDirectory is not a directory or does not exist
     * @throws UncheckedIOException
     *             if output directory creation fails or other
     *             {@link IOException} occurs
     */
    public Map<String, File> transpileFiles(File es6SourceDirectory,
            File outputDirectory, boolean skipEs5) {
        LOGGER.info("Processing frontend files from '{}'", es6SourceDirectory);
        try {
            FileUtils.forceMkdir(Objects.requireNonNull(outputDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to create output directory '%s'",
                            outputDirectory),
                    e);
        }

        if (!Objects.requireNonNull(es6SourceDirectory).isDirectory()) {
            throw new IllegalArgumentException(String.format(
                    "es6SourceDirectory '%s' is not a directory or does not exist",
                    es6SourceDirectory));
        }

        ImmutableMap.Builder<String, String> gulpFileParameters = new ImmutableMap.Builder<String, String>()
                .put("{es6_source_directory}",
                        es6SourceDirectory.getAbsolutePath())
                .put("{target_directory}", outputDirectory.getAbsolutePath())
                .put("{es5_configuration_name}", es5OutputDirectoryName)
                .put("{es6_configuration_name}", es6OutputDirectoryName)
                .put("{bundle}",
                        Boolean.toString(frontendDataProvider.shouldBundle()))
                .put("{minify}",
                        Boolean.toString(frontendDataProvider.shouldMinify()))
                .put("{hash}",
                        Boolean.toString(frontendDataProvider.shouldHash()))
                .put("{shell_file}",
                        frontendDataProvider.createShellFile(workingDirectory))
                .put("{fragment_files}",
                        combineFilePathsIntoString(frontendDataProvider
                                .createFragmentFiles(workingDirectory)));
        createFileFromTemplateResource("gulpfile.js",
                gulpFileParameters.build());

        Map<String, File> transpilationResults = new HashMap<>();
        try {
            runnerManager.getGulpRunner().execute("build_es6",
                    Collections.emptyMap());
            addTranspilationResult(transpilationResults, outputDirectory,
                    es6OutputDirectoryName);

            if (!skipEs5) {
                runnerManager.getGulpRunner().execute("build_es5",
                        Collections.emptyMap());
                addTranspilationResult(transpilationResults, outputDirectory,
                        es5OutputDirectoryName);
            }
        } catch (TaskRunnerException e) {
            throw new IllegalStateException(
                    "Transpilation with gulp has failed", e);
        }

        return transpilationResults;
    }

    private String combineFilePathsIntoString(Set<String> fragmentFiles) {
        return fragmentFiles.stream().map(fileName -> "'" + fileName + "'")
                .collect(Collectors.joining(", "));
    }

    private void addTranspilationResult(Map<String, File> transpilationResults,
            File outputDirectory, String configurationName) {
        File configurationOutput = FileUtils.getFile(outputDirectory,
                configurationName);
        if (!configurationOutput.isDirectory()) {
            throw new IllegalStateException(String.format(
                    "Unable to find transpilation result directory at '%s'",
                    configurationOutput));
        }
        transpilationResults.put(configurationName, configurationOutput);
    }
}
