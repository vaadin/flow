/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

public class TaskUpdateWebpackTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdateWebpack webpackUpdater;
    private File webpackConfig;
    private File webpackGenerated;
    private File baseDir;
    private File frontendFolder;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();
        frontendFolder = new File(baseDir, "frontend");

        NodeUpdateTestUtil.createStubNode(true, true,
                baseDir.getAbsolutePath());

        webpackUpdater = new TaskUpdateWebpack(frontendFolder, baseDir,
                new File(baseDir, TARGET + "classes"), WEBPACK_CONFIG,
                WEBPACK_GENERATED,
                new File(baseDir, DEFAULT_GENERATED_DIR + IMPORTS_NAME));

        webpackConfig = new File(baseDir, WEBPACK_CONFIG);
        webpackGenerated = new File(baseDir, WEBPACK_GENERATED);
    }

    @After
    public void teardown() {
        webpackConfig.delete();
        webpackGenerated.delete();
    }

    @Test
    public void should_CreateWebpackConfigAndGeneratedConfig()
            throws Exception {
        Assert.assertFalse("No webpack config file should be present.",
                webpackConfig.exists());
        Assert.assertFalse("No generated config file should be present.",
                webpackGenerated.exists());
        webpackUpdater.execute();

        Assert.assertTrue("webpack.config.js was not created.",
                webpackConfig.exists());
        Assert.assertTrue("webpack.generated.js was not created.",
                webpackGenerated.exists());
        assertWebpackConfigContent();
        assertWebpackGeneratedConfigContent(
                "target/frontend/generated-flow-imports.js", "target/classes");
    }

    @Test
    public void execute_webpackGeneratedConfigContainsCustomFrontendDir()
            throws Exception {
        Assert.assertFalse("No webpack config file should be present.",
                webpackConfig.exists());
        Assert.assertFalse("No generated config file should be present.",
                webpackGenerated.exists());

        frontendFolder = new File(baseDir, "my-custom-frontend");
        webpackUpdater = new TaskUpdateWebpack(frontendFolder, baseDir,
                new File(baseDir, TARGET + "classes"), WEBPACK_CONFIG,
                WEBPACK_GENERATED,
                new File(baseDir, DEFAULT_GENERATED_DIR + IMPORTS_NAME));

        webpackUpdater.execute();

        Assert.assertTrue("webpack.config.js was not created.",
                webpackConfig.exists());
        Assert.assertTrue("webpack.generated.js was not created.",
                webpackGenerated.exists());
        assertWebpackConfigContent();
        Set<String> webpackContents = Files.lines(webpackGenerated.toPath())
                .map(String::trim).collect(Collectors.toSet());

        Assert.assertTrue(webpackContents.contains(
                "const frontendFolder = require('path').resolve(__dirname, 'my-custom-frontend');"));
    }

    @Test
    public void should_updateOnlyGeneratedWebpack() throws Exception {
        Assert.assertFalse("No webpack config file should be present.",
                webpackConfig.exists());
        Assert.assertFalse("No generated config file should be present.",
                webpackGenerated.exists());

        webpackUpdater.execute();
        Assert.assertTrue("webpack.config.js was not created.",
                webpackConfig.exists());
        Assert.assertTrue("webpack.generated.js was not created.",
                webpackGenerated.exists());
        assertWebpackConfigContent();

        // Add a custom line into webpack.config.js
        String customString = "custom element;";
        List<String> lines = FileUtils.readLines(webpackConfig, "UTF-8");
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals("module.exports = merge(flowDefaults, {")) {
                lines.add(i + 1, customString);
                break;
            }
        }

        FileUtils.writeLines(webpackConfig, lines);

        TaskUpdateWebpack newUpdater = new TaskUpdateWebpack(frontendFolder,
                baseDir, new File(baseDir, "foo"), WEBPACK_CONFIG,
                WEBPACK_GENERATED, new File(baseDir, "bar"));
        newUpdater.execute();

        assertWebpackGeneratedConfigContent("bar", "foo");
        List<String> webpackContents = Files.lines(webpackConfig.toPath())
                .collect(Collectors.toList());

        Assert.assertTrue("Custom string has disappeared",
                webpackContents.contains(customString));

    }

    private void assertWebpackGeneratedConfigContent(String entryPoint,
            String outputFolder) throws IOException {

        List<String> webpackContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.toList());

        Assert.assertFalse(
                "webpack.generated.js config should not contain Windows path separators",
                webpackContents.contains("\\\\"));

        verifyNoAbsolutePathsPresent(webpackContents);

        verifyUpdate(webpackContents, entryPoint, outputFolder);
    }

    private void assertWebpackConfigContent() throws IOException {

        List<String> webpackContents = Files.lines(webpackConfig.toPath())
                .collect(Collectors.toList());

        Assert.assertTrue("No webpack-merge imported.", webpackContents
                .contains("const merge = require('webpack-merge');"));
        Assert.assertTrue("No flowDefaults imported.", webpackContents.contains(
                "const flowDefaults = require('./webpack.generated.js');"));

        Assert.assertTrue("No module.exports for flowDefaults available.",
                webpackContents
                        .contains("module.exports = merge(flowDefaults, {"));
    }

    private void verifyUpdate(List<String> webpackContents, String entryPoint,
            String outputFolder) {
        Assert.assertTrue(
                "webpack config should update fileNameOfTheFlowGeneratedMainEntryPoint",
                webpackContents.contains(
                        "const fileNameOfTheFlowGeneratedMainEntryPoint = require('path').resolve(__dirname, '"
                                + entryPoint + "');"));

        Assert.assertTrue(
                "webpack config should update fileNameOfTheFlowGeneratedMainEntryPoint",
                webpackContents.contains(
                        "const mavenOutputFolderForFlowBundledFiles = require('path').resolve(__dirname, '"
                                + outputFolder + "');"));

    }

    private void verifyNoAbsolutePathsPresent(List<String> webpackContents) {
        List<String> wrongLines = webpackContents.stream()
                // check the lines with slashes only
                .filter(line -> line.contains("/"))
                // trim the whitespaces
                .map(line -> line.replaceAll("\\s", ""))
                // publicPath is URI which should start with slash
                .filter(line -> !line.startsWith("publicPath:"))
                // check the equals ( a=something ) and object declarations (
                // {a: something} )
                .map(line -> {
                    int equalsSignPosition = line.indexOf("=");
                    int jsonPropertySignPosition = line.indexOf(":");
                    if (equalsSignPosition > 0) {
                        return line.substring(equalsSignPosition + 1);
                    } else if (jsonPropertySignPosition > 0) {
                        return line.substring(jsonPropertySignPosition + 1);
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull)
                // take the lines with strings only and trim the string start
                .filter(line -> line.startsWith("'") || line.startsWith("\"")
                        || line.startsWith("`"))
                .map(line -> line.substring(1))
                .filter(line -> line.startsWith("/"))
                .collect(Collectors.toList());

        Assert.assertTrue(String.format(
                "Expected to have no lines that have a string starting with a slash in assignment. Incorrect lines: '%s'",
                wrongLines), wrongLines.isEmpty());
    }
}
