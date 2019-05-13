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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.NodeUpdater.log;
/**
 * Updates the webpack config file according with current project settings.
 */
public class TaskUpdateWebpack implements Command {

    /**
     * The name of the webpack config file.
     */
    private final String webpackTemplate;
    private final transient Path webpackOutputDirectory;
    private final transient Path generatedFlowImports;
    private final transient Path webpackConfigFolder;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param webpackConfigFolder
     *            folder with the `webpack.config.js` file.
     * @param webpackOutputDirectory
     *            the directory to set for webpack to output its build results.
     * @param webpackTemplate
     *            name of the webpack resource to be used as template when
     *            creating the <code>webpack.config.js</code> file.
     * @param generatedFlowImports
     *            name of the JS file to update with the Flow project imports
     */
    public TaskUpdateWebpack(File webpackConfigFolder, File webpackOutputDirectory,
            String webpackTemplate, File generatedFlowImports) {
        this.webpackTemplate = webpackTemplate;
        this.webpackOutputDirectory = webpackOutputDirectory.toPath();
        this.generatedFlowImports = generatedFlowImports.toPath();
        this.webpackConfigFolder = webpackConfigFolder.toPath();
    }

    @Override
    public void execute() {
        try {
            createWebpackConfig();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createWebpackConfig() throws IOException {
        if (webpackTemplate == null || webpackTemplate.trim().isEmpty()) {
            return;
        }

        File configFile = new File(webpackConfigFolder.toFile(),
                WEBPACK_CONFIG);

        if (!configFile.exists()) {
            URL resource = this.getClass().getClassLoader()
                    .getResource(webpackTemplate);
            FileUtils.copyURLToFile(resource, configFile);
            log().info("Created webpack file: " + configFile);
        }

        if (configFile.exists()) {
            List<String> lines = FileUtils.readLines(configFile, "UTF-8");

            boolean modified = false;
            String outputLine = "mavenOutputFolderForFlowBundledFiles = require('path').resolve(__dirname, '"
                    + getEscapedRelativeWebpackPath(webpackOutputDirectory) + "');";
            String mainLine = "fileNameOfTheFlowGeneratedMainEntryPoint = require('path').resolve(__dirname, '"
                    + getEscapedRelativeWebpackPath(generatedFlowImports) + "');";

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (lines.get(i).startsWith("fileNameOfTheFlowGeneratedMainEntryPoint") && !line.equals(mainLine)) {
                    lines.set(i, mainLine);
                    modified = true;
                }
                if (lines.get(i).startsWith("mavenOutputFolderForFlowBundledFiles") && !line.equals(outputLine)) {
                    lines.set(i, outputLine);
                    modified = true;
                }
            }

            if (modified) {
                FileUtils.writeLines(configFile, lines);
                log().info("Updated webpack file: " + configFile);
            }
        }
    }

    private String getEscapedRelativeWebpackPath(Path path) {
        Path relativePath = path.isAbsolute()
                ? webpackConfigFolder.relativize(path)
                : path;
        return relativePath.toString().replaceAll("\\\\", "/");
    }
}
