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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.APPLICATION_THEME_ROOT;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_FILES_PATH;

/**
 * Copies production bundle files from pre-compiled bundle JAR into a folder
 * where production bundle is normally located.
 * <p>
 * Copies project's custom theme files to the resources output folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 */
public class TaskPrepareProdBundle implements FallibleCommand {

    private final Options options;

    public TaskPrepareProdBundle(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (hasProdBundle()) {
            ProdBundleUtils.unpackBundle(options.getNpmFolder(),
                    options.getResourceOutputDirectory());
        } else {
            copyDefaultBundleFilesFromJar();
        }
        copyProjectThemes();
        writePreCompiledMarker();
    }

    private void copyProjectThemes() {
        File localThemesRoot = new File(options.getFrontendDirectory(),
                APPLICATION_THEME_ROOT);
        if (localThemesRoot.exists()) {
            File webappResourcesDirectory = options
                    .getWebappResourcesDirectory();
            if (webappResourcesDirectory == null) {
                String buildDirectory = options.getBuildDirectoryName();
                webappResourcesDirectory = new File(buildDirectory,
                        Paths.get("classes", Constants.VAADIN_WEBAPP_RESOURCES)
                                .toString());
            }
            File target = new File(webappResourcesDirectory,
                    Paths.get(VAADIN_STATIC_FILES_PATH, APPLICATION_THEME_ROOT)
                            .toString());
            File[] localThemes = localThemesRoot.listFiles(File::isDirectory);
            if (localThemes == null) {
                throw new IllegalStateException();
            }
            for (File theme : localThemes) {
                try {
                    FileIOUtils.copyDirectory(theme,
                            new File(target, theme.getName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void copyDefaultBundleFilesFromJar()
            throws ExecutionFailedException {
        URL statsJson = BundleValidationUtil.getProdBundleResource(
                "config/stats.json", options.getClassFinder());
        if (statsJson == null) {
            throw new IllegalStateException(
                    "Could not copy production bundle files, because couldn't find production bundle in the class-path");
        }
        String pathToJar = statsJson.getPath();
        int index = pathToJar.lastIndexOf(".jar!/");
        if (index >= 0) {
            // exclude relative path starting from !/
            pathToJar = pathToJar.substring(0, index + 4);
        }
        try {
            URI jarUri = new URI(pathToJar);
            JarContentsManager jarContentsManager = new JarContentsManager();
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                    new File(jarUri), Constants.PROD_BUNDLE_NAME,
                    options.getResourceOutputDirectory(), "**/*.*");
        } catch (URISyntaxException e) {
            throw new ExecutionFailedException(
                    "Couldn't copy production bundle files", e);
        }
    }

    private boolean hasProdBundle() {
        File prodBundleFolder = new File(options.getNpmFolder(),
                Constants.PROD_BUNDLE_COMPRESSED_FILE_LOCATION);
        return prodBundleFolder.exists();
    }

    private void writePreCompiledMarker() throws ExecutionFailedException {
        try {
            File statsJsonFile = new File(options.getResourceOutputDirectory(),
                    "config/stats.json");
            ObjectNode statsJsonContent = JacksonUtils
                    .readTree(Files.readString(statsJsonFile.toPath(),
                            StandardCharsets.UTF_8));
            statsJsonContent.put("pre-compiled", true);
            Files.writeString(statsJsonFile.toPath(), statsJsonContent + "\n");
        } catch (IOException e) {
            throw new ExecutionFailedException(
                    "Couldn't access stats.json file", e);
        }
    }
}
