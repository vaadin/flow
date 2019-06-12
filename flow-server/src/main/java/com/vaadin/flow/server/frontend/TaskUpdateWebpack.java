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
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;
import static com.vaadin.flow.server.frontend.NodeUpdater.log;
/**
 * Updates the webpack config file according with current project settings.
 */
public class TaskUpdateWebpack implements Command {

    /**
     * The name of the webpack config file.
     */
    private final String webpackTemplate;
    private final String webpackGeneratedTemplate;
    private final transient Path webpackOutputPath;
    private final transient Path flowImportsFilePath;
    private final transient Path webpackConfigPath;

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
     * @param webpackGeneratedTemplate
     *            name of the webpack resource to be used as template when
     *            creating the <code>webpack.generated.js</code> file.
     * @param generatedFlowImports
     *            name of the JS file to update with the Flow project imports
     */
    TaskUpdateWebpack(File webpackConfigFolder, File webpackOutputDirectory,
            String webpackTemplate, String webpackGeneratedTemplate,
            File generatedFlowImports) {
        this.webpackTemplate = webpackTemplate;
        this.webpackGeneratedTemplate = webpackGeneratedTemplate;
        this.webpackOutputPath = webpackOutputDirectory.toPath();
        this.flowImportsFilePath = generatedFlowImports.toPath();
        this.webpackConfigPath = webpackConfigFolder.toPath();
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

        File configFile = new File(webpackConfigPath.toFile(), WEBPACK_CONFIG);

        // If we have an old config file we remove it and create the new one using the webpack.generated.js
        if (configFile.exists()) {
            if (!FileUtils.readFileToString(configFile, "UTF-8").contains(
                    "const flowDefaults = require('./webpack.generated.js');")) {
                log().info("Webpack configuration was outdated, removing file: " + configFile);
                configFile.delete();
            }
        }

        if (!configFile.exists()) {
            URL resource = this.getClass().getClassLoader()
                    .getResource(webpackTemplate);
            FileUtils.copyURLToFile(resource, configFile);
            log().info("Created webpack configuration file: " + configFile);
        }

        // Generated file is always re-written
        File generatedFile = new File(webpackConfigPath.toFile(), WEBPACK_GENERATED);

        URL resource = this.getClass().getClassLoader()
                .getResource(webpackGeneratedTemplate);
        FileUtils.copyURLToFile(resource, generatedFile);
        List<String> lines = FileUtils.readLines(generatedFile, "UTF-8");

        String outputLine =
                "mavenOutputFolderForFlowBundledFiles = require('path').resolve(__dirname, '"
                        + getEscapedRelativeWebpackPath(webpackOutputPath)
                        + "');";
        String mainLine =
                "fileNameOfTheFlowGeneratedMainEntryPoint = require('path').resolve(__dirname, '"
                        + getEscapedRelativeWebpackPath(flowImportsFilePath)
                        + "');";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (lines.get(i)
                    .startsWith("fileNameOfTheFlowGeneratedMainEntryPoint")
                    && !line.equals(mainLine)) {
                lines.set(i, mainLine);
            }
            if (lines.get(i).startsWith("mavenOutputFolderForFlowBundledFiles")
                    && !line.equals(outputLine)) {
                lines.set(i, outputLine);
            }
        }

        FileUtils.writeLines(generatedFile, lines);
    }

    private String getEscapedRelativeWebpackPath(Path path) {
        Path relativePath = path.isAbsolute()
                ? webpackConfigPath.relativize(path)
                : path;
        return relativePath.toString().replaceAll("\\\\", "/");
    }
}
