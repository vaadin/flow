/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_FILES_PATH;
import static elemental.json.impl.JsonUtil.stringify;

/**
 * Creates a flow-settings.json file for use with dev server configuration.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskUpdateSettingsFile implements FallibleCommand, Serializable {

    File npmFolder;
    File frontendDirectory;
    File generatedFolder;
    File webpackOutputDirectory;
    String buildDirectory;

    TaskUpdateSettingsFile(NodeTasks.Builder builder) {
        this.npmFolder = builder.getNpmFolder();
        this.frontendDirectory = builder.getFrontendDirectory();
        this.generatedFolder = builder.getGeneratedFolder();
        this.webpackOutputDirectory = builder.getWebpackOutputDirectory();
        this.buildDirectory = builder.getBuildDirectory();
    }

    @Override
    public void execute() {
        if (npmFolder == null)
            return;

        JsonObject settings = Json.createObject();
        settings.put("frontendFolder", getRelativePath(npmFolder.toPath(),
                frontendDirectory.toPath()));
        settings.put("themeFolder", "themes");
        settings.put("themeResourceFolder",
                getRelativePath(npmFolder.toPath(), generatedFolder.toPath()));
        String output;
        if (webpackOutputDirectory == null) {
            output = Paths.get(buildDirectory, VAADIN_WEBAPP_RESOURCES,
                    VAADIN_STATIC_FILES_PATH).toString();
        } else
            output = webpackOutputDirectory + VAADIN_STATIC_FILES_PATH;

        settings.put("staticOutput",
                getRelativePath(npmFolder.toPath(), new File(output).toPath()));
        settings.put("generatedFolder", "generated");

        File settingsFile = new File(npmFolder,
                buildDirectory + "/flow-settings.json");

        try {
            FileUtils.write(settingsFile, stringify(settings, 2),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log().error("Failed to write file: {}", settingsFile);
            log().trace("Failed to write settings file", e);
        }
    }

    private String getRelativePath(Path source, Path path) {
        if (path.isAbsolute()) {
            return FrontendUtils.getUnixRelativePath(source, path);
        } else {
            return FrontendUtils.getUnixPath(path);
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
