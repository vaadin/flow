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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.testutil.FrontendStubs;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

public class TaskUpdateWebpackTest extends NodeUpdateTestUtil {

    @PWA(name = "foo", shortName = "bar", offlineResources = { "foo.css",
            "bar.js" })
    class AppShell {
    }

    @PWA(name = "foo", shortName = "bar", offlinePath = "off.html")
    class AppShellWithOfflinePath {
    }

    @PWA(name = "foo", shortName = "bar", offline = false)
    class AppShellWithDisabledOffline {
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdateWebpack webpackUpdater;
    private File webpackConfig;
    private File webpackGenerated;
    private File baseDir;
    private File frontendFolder;
    private File frontendGeneratedFolder;
    private PwaConfiguration pwaConfiguration;
    private boolean useV14Bootstrapping = false;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();
        frontendFolder = new File(baseDir, "frontend");
        frontendGeneratedFolder = new File(baseDir,
                DEFAULT_PROJECT_FRONTEND_GENERATED_DIR);

        FrontendStubs.createStubNode(true, true, baseDir.getAbsolutePath());

        pwaConfiguration = new PwaConfiguration(
                AppShell.class.getAnnotation(PWA.class));

        createWebpackUpdater();

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
                "target/frontend/generated-flow-imports.js", "target/webapp",
                "target/classes");
    }

    @Test
    public void execute_webpackGeneratedConfigContainsCustomFrontendDir()
            throws Exception {
        Assert.assertFalse("No webpack config file should be present.",
                webpackConfig.exists());
        Assert.assertFalse("No generated config file should be present.",
                webpackGenerated.exists());

        frontendFolder = new File(baseDir, "my-custom-frontend");
        createWebpackUpdater();

        webpackUpdater.execute();

        Assert.assertTrue("webpack.config.js was not created.",
                webpackConfig.exists());
        Assert.assertTrue("webpack.generated.js was not created.",
                webpackGenerated.exists());
        assertWebpackConfigContent();
        Set<String> webpackContents = Files.lines(webpackGenerated.toPath())
                .map(String::trim).collect(Collectors.toSet());

        Assert.assertTrue(webpackContents.contains(
                "const frontendFolder = path.resolve(__dirname, 'my-custom-frontend');"));
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
            if (lines.get(i).equals("module.exports = merge(flowDefaults,")) {
                lines.add(i + 1, customString);
                break;
            }
        }

        FileUtils.writeLines(webpackConfig, lines);

        TaskUpdateWebpack newUpdater = new TaskUpdateWebpack(frontendFolder,
                baseDir, new File(baseDir, "baz"), new File(baseDir, "foo"),
                new File(baseDir, "bar"), false,
                new File(baseDir, Paths
                        .get(TARGET, DEFAULT_FLOW_RESOURCES_FOLDER).toString()),
                pwaConfiguration, TARGET);

        newUpdater.execute();

        assertWebpackGeneratedConfigContent("bar", "baz", "foo");
        List<String> webpackContents = Files.lines(webpackConfig.toPath())
                .collect(Collectors.toList());

        Assert.assertTrue("Custom string has disappeared",
                webpackContents.contains(customString));

    }

    @Test
    public void should_notSetClientSideBootstrapMode_when_runningV14Bootstrapping()
            throws IOException {
        useV14Bootstrapping = true;
        createWebpackUpdater();
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "useClientSideIndexFileForBootstrapping should be false by "
                        + "default",
                webpackGeneratedContents.contains(
                        "const useClientSideIndexFileForBootstrapping = false;"));
    }

    @Test
    public void should_setClientSideBootstrapMode_when_runningV15Bootstrapping()
            throws IOException {
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "useClientSideIndexFileForBootstrapping should be true",
                webpackGeneratedContents.contains(
                        "const useClientSideIndexFileForBootstrapping = true;"));

    }

    @Test
    public void should_useAppShellPath_when_defaultInPwa() throws IOException {
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue("offlinePath should be appShellPath '.' by default",
                webpackGeneratedContents.contains("const offlinePath = '.';"));
    }

    @Test
    public void should_enableCustomOfflinePath_when_customisedInPwa()
            throws IOException {
        pwaConfiguration = new PwaConfiguration(
                AppShellWithOfflinePath.class.getAnnotation(PWA.class));
        createWebpackUpdater();

        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));

        Assert.assertTrue("offlinePath should be customizable",
                webpackGeneratedContents
                        .contains("const offlinePath = 'off.html';"));
    }

    @Test
    public void should_setPwaEnabledFalse_when_noPwa() throws IOException {
        pwaConfiguration = new PwaConfiguration();
        createWebpackUpdater();
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue("pwaEnabled expected false",
                webpackGeneratedContents.contains("const pwaEnabled = false;"));
    }

    @Test
    public void should_setPwaEnabledTrue_when_Pwa() throws IOException {
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue("pwaEnabled expected true",
                webpackGeneratedContents.contains("const pwaEnabled = true;"));
    }

    @Test
    public void should_setOfflineEnabledTrue_when_defaultInPwa()
            throws IOException {
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue("offlineEnabled expected true",
                webpackGeneratedContents
                        .contains("const offlineEnabled = true;"));
    }

    @Test
    public void should_setOfflineEnabledFalse_when_customisedInPwa()
            throws IOException {
        pwaConfiguration = new PwaConfiguration(
                AppShellWithDisabledOffline.class.getAnnotation(PWA.class));
        createWebpackUpdater();

        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));

        Assert.assertTrue("offlineEnabled expected false",
                webpackGeneratedContents
                        .contains("const offlineEnabled = false;"));
    }

    @Test
    public void should_setServiceWorkerEntryPoint_fromTargetFolder_when_NoCustomServiceWorkerFileFound()
            throws IOException {
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "service workder entry point should be from target folder",
                webpackGeneratedContents.contains(
                        "const clientServiceWorkerEntryPoint = path.resolve(__dirname, 'target/sw');"));
    }

    @Test
    public void should_setServiceWorkerEntryPoint_fromCurrentFolder_when_CustomServiceWorkerFileFound()
            throws IOException {
        frontendFolder.mkdir();
        File customSWFile = new File(frontendFolder, SERVICE_WORKER_SRC);
        customSWFile.createNewFile();
        createWebpackUpdater();
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "service workder entry point should be from current folder",
                webpackGeneratedContents.contains(
                        "const clientServiceWorkerEntryPoint = './sw';"));
        customSWFile.delete();
        frontendFolder.delete();
    }

    @Test
    public void should_setServiceWorkerEntryPoint_fromCurrentFolder_when_CustomServiceWorkerJsFileFound()
            throws IOException {
        frontendFolder.mkdir();
        File customSWFile = new File(frontendFolder, SERVICE_WORKER_SRC_JS);
        customSWFile.createNewFile();
        createWebpackUpdater();
        webpackUpdater.execute();
        String webpackGeneratedContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "service workder entry point should be from current folder",
                webpackGeneratedContents.contains(
                        "const clientServiceWorkerEntryPoint = './sw';"));
        customSWFile.delete();
        frontendFolder.delete();
    }

    protected void createWebpackUpdater() {
        webpackUpdater = new TaskUpdateWebpack(frontendFolder, baseDir,
                new File(baseDir, TARGET + "/webapp"),
                new File(baseDir, TARGET + "/classes"),
                new File(baseDir,
                        Paths.get(Constants.TARGET, DEFAULT_GENERATED_DIR,
                                IMPORTS_NAME).toString()),
                useV14Bootstrapping,
                new File(baseDir,
                        Paths.get(Constants.TARGET,
                                DEFAULT_FLOW_RESOURCES_FOLDER).toString()),
                pwaConfiguration, TARGET);
    }

    private void assertWebpackGeneratedConfigContent(String entryPoint,
            String outputFolder, String resourceFolder) throws IOException {

        List<String> webpackContents = Files.lines(webpackGenerated.toPath())
                .collect(Collectors.toList());

        Assert.assertFalse(
                "webpack.generated.js config should not contain Windows path separators",
                webpackContents.contains("\\\\"));

        verifyNoAbsolutePathsPresent(webpackContents);

        verifyUpdate(webpackContents, entryPoint, outputFolder, resourceFolder);
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
                        .contains("module.exports = merge(flowDefaults,"));
    }

    private void verifyUpdate(List<String> webpackContents, String entryPoint,
            String outputFolder, String resourceFolder) {
        Assert.assertTrue(
                "webpack config should update fileNameOfTheFlowGeneratedMainEntryPoint",
                webpackContents.contains(
                        "const fileNameOfTheFlowGeneratedMainEntryPoint = path.resolve(__dirname, '"
                                + entryPoint + "');"));

        Assert.assertTrue(
                "webpack config should update mavenOutputFolderForFlowBundledFiles",
                webpackContents.contains(
                        "const mavenOutputFolderForFlowBundledFiles = path.resolve(__dirname, '"
                                + outputFolder + "');"));
        Assert.assertTrue(
                "webpack config should update mavenOutputFolderForResourceFiles",
                webpackContents.contains(
                        "const mavenOutputFolderForResourceFiles = path.resolve(__dirname, '"
                                + resourceFolder + "');"));

    }

    private void verifyNoAbsolutePathsPresent(List<String> webpackContents) {
        List<String> wrongLines = webpackContents.stream()
                // check the lines with slashes only
                .filter(line -> line.contains("/"))
                // trim the whitespaces
                .map(line -> line.replaceAll("\\s", ""))
                // rootUrl is URI which should start with slash
                .filter(line -> !line.startsWith("constrootUrl="))
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
