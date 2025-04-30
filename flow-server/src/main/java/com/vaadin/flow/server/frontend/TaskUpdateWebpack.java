/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_FILES_PATH;

/**
 * Updates the webpack config file according with current project settings.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskUpdateWebpack implements FallibleCommand {

    /**
     * The name of the webpack config file.
     */
    private final String webpackTemplate;
    private final String webpackGeneratedTemplate;
    private final Path webpackOutputPath;
    private final Path flowImportsFilePath;
    private final Path webpackConfigPath;
    private final Path frontendDirectory;
    private final Path flowResourcesFolder;
    private final Path resourceFolder;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param frontendDirectory
     *            the directory used for {@code Frontend} alias
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
    TaskUpdateWebpack(File frontendDirectory, File webpackConfigFolder,
            File webpackOutputDirectory, String webpackTemplate,
            String webpackGeneratedTemplate, File generatedFlowImports) {
        this.frontendDirectory = frontendDirectory.toPath();
        this.webpackTemplate = webpackTemplate;
        this.webpackGeneratedTemplate = webpackGeneratedTemplate;
        this.webpackOutputPath = webpackOutputDirectory.toPath();
        this.flowImportsFilePath = generatedFlowImports.toPath();
        this.webpackConfigPath = webpackConfigFolder.toPath();
        this.flowResourcesFolder = new File(webpackConfigFolder,
                System.getProperty(PARAM_GENERATED_DIR, DEFAULT_GENERATED_DIR))
                .toPath();
        this.resourceFolder = new File(webpackOutputDirectory.getParentFile(),
                VAADIN_STATIC_FILES_PATH).toPath();
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
        if (configFile.exists()) {
            if (!FrontendUtils.isWebpackConfigFile(configFile)) {
                log().warn(
                        "Flow generated webpack configuration was not mentioned "
                                + "in the configuration file: {}."
                                + "Please verify that './webpack.generated.js' is used "
                                + "in the merge or remove the file to generate a new one.",
                        configFile);
            }
            // webpack-merge-plugin 5 changes the import format
            String contents = FileUtils.readFileToString(configFile,
                    StandardCharsets.UTF_8);
            if (contents.contains("const merge = require('webpack-merge');")) {
                log().debug("Updating " + configFile.getName()
                        + " to new webpack-merge syntax");
                contents = contents.replace(
                        "const merge = require('webpack-merge');",
                        "const { merge } = require('webpack-merge');");
                FileUtils.write(configFile, contents, StandardCharsets.UTF_8);
            }

        } else {
            URL resource = this.getClass().getClassLoader()
                    .getResource(webpackTemplate);
            FileUtils.copyURLToFile(resource, configFile);
            log().debug("Created webpack configuration file: " + configFile);
        }

        // Generated file is always re-written
        File generatedFile = new File(webpackConfigPath.toFile(),
                WEBPACK_GENERATED);

        URL resource = this.getClass().getClassLoader()
                .getResource(webpackGeneratedTemplate);
        FileUtils.copyURLToFile(resource, generatedFile);
        List<String> lines = FileUtils.readLines(generatedFile, "UTF-8");

        String frontendLine = "const frontendFolder = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(frontendDirectory) + "');";

        String outputLine = "const mavenOutputFolderForFlowBundledFiles = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(webpackOutputPath) + "');";
        String mainLine = "const fileNameOfTheFlowGeneratedMainEntryPoint = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(flowImportsFilePath) + "');";
        String devmodeGizmoJSLine = "const devmodeGizmoJS = '"
                + FrontendUtils.DEVMODE_GIZMO_MODULE + "'";

        String frontendFolder = "const flowFrontendFolder = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(flowResourcesFolder) + "');";
        String assetsResourceFolder = "const projectStaticAssetsOutputFolder = require('path').resolve(__dirname, '"
                + getEscapedRelativeWebpackPath(resourceFolder) + "');";

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
            if (lines.get(i).startsWith("const frontendFolder")) {
                lines.set(i, frontendLine);
            }

            if (lines.get(i).startsWith("const devmodeGizmoJS")) {
                lines.set(i, devmodeGizmoJSLine);
            }
            if (lines.get(i).startsWith("const flowFrontendFolder")) {
                lines.set(i, frontendFolder);
            }
            if (lines.get(i)
                    .startsWith("const projectStaticAssetsOutputFolder")) {
                lines.set(i, assetsResourceFolder);
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

    private Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }
}
