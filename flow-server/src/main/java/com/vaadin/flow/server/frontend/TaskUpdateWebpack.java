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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.FallibleCommand;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

/**
 * Updates the webpack config file according with current project settings.
 */
public class TaskUpdateWebpack implements FallibleCommand {

    static final String PLUGINS_PLACEHOLDER = "//to-be-inserted-plugins";
    static final String PLUGIN_IMPORTS_PLACEHOLDER = "//to-be-inserted-imports";
    /**
     * The name of the webpack config file.
     */
    private final String webpackTemplate;
    private final String webpackGeneratedTemplate;
    private final transient Set<WebpackPlugin> plugins;
    private final transient Path webpackOutputPath;
    private final transient Path webpackEntryPoint;
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
     * @param webpackEntryPoint
     *            name of the file to update as the entry point in webpack
     *            config
     */
    TaskUpdateWebpack(File webpackConfigFolder, File webpackOutputDirectory,
            String webpackTemplate, String webpackGeneratedTemplate,
            File webpackEntryPoint) {
        this(webpackConfigFolder, webpackOutputDirectory, webpackTemplate,
                webpackGeneratedTemplate, webpackEntryPoint,
                Collections.emptySet());
    }

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
     * @param webpackEntryPoint
     *            name of the file to update as the entry point in webpack
     *            config
     * @param plugins
     *            A list of webpack plugins should be inserted
     */
    TaskUpdateWebpack(File webpackConfigFolder, File webpackOutputDirectory,
            String webpackTemplate, String webpackGeneratedTemplate,
            File webpackEntryPoint, Set<WebpackPlugin> plugins) {
        this.webpackTemplate = webpackTemplate;
        this.webpackGeneratedTemplate = webpackGeneratedTemplate;
        this.webpackOutputPath = webpackOutputDirectory.toPath();
        this.webpackEntryPoint = webpackEntryPoint.toPath();
        this.webpackConfigPath = webpackConfigFolder.toPath();
        this.plugins = plugins;
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

        // If we have an old config file we remove it and create the new one
        // using the webpack.generated.js
        if (configFile.exists()
                && !FileUtils.readFileToString(configFile, "UTF-8")
                    .contains("./webpack.generated.js")) {
                log().warn(
                        "Flow generated webpack configuration was not mentioned "
                                + "in the configuration file: {}."
                                + "Please verify that './webpack.generated.js' is used "
                                + "in the merge or remove the file to generate a new one.",
                        configFile);
        }

        if (!configFile.exists()) {
            URL resource = this.getClass().getClassLoader()
                    .getResource(webpackTemplate);
            FileUtils.copyURLToFile(resource, configFile);
            log().info("Created webpack configuration file: '{}'", configFile);
        }

        // Generated file is always re-written
        File generatedFile = new File(webpackConfigPath.toFile(),
                WEBPACK_GENERATED);

        URL resource = this.getClass().getClassLoader()
                .getResource(webpackGeneratedTemplate);
        FileUtils.copyURLToFile(resource, generatedFile);
        List<String> lines = modifyWebpackConfig(generatedFile);

        FileUtils.writeLines(generatedFile, lines);
    }

    private List<String> modifyWebpackConfig(File generatedFile)
            throws IOException {
        List<String> lines = FileUtils.readLines(generatedFile, "UTF-8");

        String outputLine = "const mavenOutputFolderForFlowBundledFiles = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(webpackOutputPath) + "');";
        String mainLine = "const fileNameOfTheFlowGeneratedMainEntryPoint = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(webpackEntryPoint) + "');";
        Set<String> imports = plugins.stream()
                .map(WebpackPlugin::getImportStatement)
                .collect(Collectors.toSet());
        String pluginsContent = plugins.stream().map(WebpackPlugin::getContent)
                .collect(Collectors.joining(",\n\n"));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (lines.get(i).startsWith(
                    "const fileNameOfTheFlowGeneratedMainEntryPoint")
                    && !line.equals(mainLine)) {
                lines.set(i, mainLine);
            }
            if (lines.get(i)
                    .startsWith("const mavenOutputFolderForFlowBundledFiles")
                    && !line.equals(outputLine)) {
                lines.set(i, outputLine);
            }

            // remove duplicated imports
            imports.remove(lines.get(i));

            if (lines.get(i).startsWith(PLUGIN_IMPORTS_PLACEHOLDER)) {
                lines.set(i, String.join("\n", imports));
            }
            if (lines.get(i).startsWith(PLUGINS_PLACEHOLDER)) {
                lines.set(i, pluginsContent);
            }
        }
        return lines;
    }

    private String getEscapedRelativeWebpackPath(Path path) {
        Path relativePath = path.isAbsolute()
                ? webpackConfigPath.relativize(path)
                : path;
        return relativePath.toString().replaceAll("\\\\", "/");
    }

    private Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }
}
