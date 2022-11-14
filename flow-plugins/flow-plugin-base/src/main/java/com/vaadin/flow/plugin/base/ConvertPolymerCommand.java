/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.plugin.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.FrontendUtils.CommandExecutionException;
import com.vaadin.flow.polymer2lit.FrontendConverter;
import com.vaadin.flow.polymer2lit.ServerConverter;

/**
 * A tool-independent implementation of a {@code convert-polymer} command that
 * converts Polymer-based source files to Lit. The command is supposed to be
 * called by the corresponding Mojo and Gradle tasks.
 */
public class ConvertPolymerCommand implements AutoCloseable {
    private static final String SERVER_GLOB = "**/*.java";
    private static final String FRONTEND_GLOB = "**/*.js";

    /**
     * A reference to the plugin adapter providing access to the environment of
     * the tool that runs the command.
     */
    private PluginAdapterBase adapter;

    /**
     * A path to a specific file or directory that needs to be converted. By
     * default, the converter scans and tries to convert all {@code *.js} and
     * {@code *.java} files in the project except for the {@code node_modules}
     * folder.
     */
    private String path;

    /**
     * Whether to enforce Lit 1 compatible imports.
     */
    private boolean useLit1;

    /**
     * Whether to disable the usage of the JavaScript optional chaining operator
     * (?.) in the output.
     */
    private boolean disableOptionalChaining;

    /**
     * A server converter instance for converting *.java files.
     */
    private ServerConverter serverConverter;

    /**
     * A frontend converter instance for converting *.js files.
     */
    private FrontendConverter frontendConverter;

    public ConvertPolymerCommand(PluginAdapterBase adapter, String path,
            boolean useLit1, boolean disableOptionalChaining)
            throws URISyntaxException, IOException {
        this.adapter = adapter;
        this.path = path;
        this.useLit1 = useLit1;
        this.disableOptionalChaining = disableOptionalChaining;
        this.serverConverter = new ServerConverter();
        this.frontendConverter = new FrontendConverter(
                new FrontendTools(getFrontendToolsSettings()));
    }

    @Override
    public void close() throws IOException {
        this.frontendConverter.close();
    }

    public void execute() throws IOException, InterruptedException,
            CommandExecutionException {
        Path lookupPath = getLookupPath();

        for (Path filePath : getFilePathsByGlob(lookupPath, SERVER_GLOB)) {
            try {
                adapter.logInfo(String.format("Processing %s...", filePath));
                if (serverConverter.convertFile(filePath)) {
                    adapter.logInfo(
                            "The file has been successfully converted.");
                } else {
                    adapter.logInfo(
                            "No occurences of PolymerTemplate was found. Skipping.");
                }
            } catch (Exception e) {
                adapter.logError("An error occurred while processing.", e);
            }
        }

        for (Path filePath : getFilePathsByGlob(lookupPath, FRONTEND_GLOB)) {
            try {
                adapter.logInfo(String.format("Processing %s...", filePath));
                if (frontendConverter.convertFile(filePath, useLit1,
                        disableOptionalChaining)) {
                    adapter.logInfo(
                            "The file has been successfully converted.");
                } else {
                    adapter.logInfo(
                            "No occurences of PolymerElement was found. Skipping.");
                }
            } catch (Exception e) {
                adapter.logError("An error occurred while processing.", e);
            }

        }
    }

    private List<Path> getFilePathsByGlob(Path baseDir, String glob)
            throws IOException {
        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(baseDir)) {
            return walk.filter(path -> matcher.matches(path))
                    .filter(path -> !path.toString().contains("node_modules"))
                    .collect(Collectors.toList());
        }
    }

    private Path getLookupPath() {
        if (path != null) {
            return Paths.get(adapter.projectBaseDirectory().toString(), path);
        }

        return adapter.projectBaseDirectory();
    }

    private FrontendToolsSettings getFrontendToolsSettings()
            throws URISyntaxException {
        FrontendToolsSettings settings = new FrontendToolsSettings(
                adapter.npmFolder().getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(adapter.nodeDownloadRoot());
        settings.setNodeVersion(adapter.nodeVersion());
        settings.setAutoUpdate(adapter.nodeAutoUpdate());
        settings.setUseGlobalPnpm(adapter.useGlobalPnpm());
        settings.setForceAlternativeNode(adapter.requireHomeNodeExec());
        return settings;
    }
}
