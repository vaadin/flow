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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.FrontendPluginsUtil.PLUGIN_TARGET;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TaskInstallFrontendBuildPluginsTest {

    public static final String BUILD_DIRECTORY = TARGET;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File rootFolder;

    private TaskInstallFrontendBuildPlugins task;

    @Before
    public void init() throws IOException {
        rootFolder = temporaryFolder.newFolder();
        Options options = new Options(Mockito.mock(Lookup.class), rootFolder)
                .withBuildDirectory(TARGET);
        task = new TaskInstallFrontendBuildPlugins(options);
    }

    @Test
    public void getPluginsReturnsExpectedList() {
        String[] expectedPlugins = new String[] { "application-theme-plugin",
                "theme-loader", "theme-live-reload-plugin",
                "build-status-plugin", "rollup-plugin-postcss-lit-custom",
                "react-function-location-plugin", "rollup-plugin-vaadin-i18n",
                "vite-plugin-service-worker", "react-component-properties-plugin" };
        final List<String> plugins = FrontendPluginsUtil.getPlugins();
        Assert.assertEquals("Unexpected number of plugins in 'plugins.json'",
                expectedPlugins.length, plugins.size());

        for (String plugin : expectedPlugins) {
            Assert.assertTrue("'plugins.json' didn't contain '" + plugin + "'",
                    plugins.contains(plugin));
        }
    }

    @Test
    public void webpackPluginsAreCopied() throws IOException {
        task.execute();

        assertPlugins();
    }

    @Test
    public void pluginsDefineAllScriptFiles() throws IOException {
        for (String plugin : FrontendPluginsUtil.getPlugins()) {
            verifyPluginScriptFilesAreDefined(plugin);
        }
    }

    @Test
    public void pluginsNotAddedToPackageJson() throws IOException {
        Options options = new MockOptions(rootFolder)
                .withBuildDirectory(BUILD_DIRECTORY);
        NodeUpdater nodeUpdater = new NodeUpdater(
                Mockito.mock(FrontendDependencies.class), options) {
            @Override
            public void execute() {
            }
        };

        task.execute();

        final JsonNode packageJson = nodeUpdater.getPackageJson();

        final JsonNode devDependencies = packageJson.get(DEV_DEPENDENCIES);
        for (String plugin : FrontendPluginsUtil.getPlugins()) {
            Assert.assertFalse("Plugin " + plugin + " added to packageJson",
                    devDependencies.has("@vaadin/" + plugin));
        }
    }

    private void assertPlugins() throws IOException {
        Assert.assertTrue("No @vaadin folder created",
                Paths.get(rootFolder.toString(), BUILD_DIRECTORY, PLUGIN_TARGET)
                        .toFile().exists());
        for (String plugin : FrontendPluginsUtil.getPlugins()) {
            assertPlugin(plugin);
        }
    }

    private void assertPlugin(String plugin) throws IOException {
        final File pluginFolder = getPluginFolder(plugin);

        final ArrayNode files = getPluginFiles(pluginFolder);
        for (int i = 0; i < files.size(); i++) {
            Assert.assertTrue(
                    "Missing plugin file " + files.get(i).textValue() + " for "
                            + plugin,
                    new File(pluginFolder, files.get(i).textValue()).exists());
        }
    }

    private void verifyPluginScriptFilesAreDefined(String plugin)
            throws IOException {
        final File pluginFolder = new File(this.getClass().getClassLoader()
                .getResource(PLUGIN_TARGET + "/" + plugin).getFile());

        final ArrayNode files = getPluginFiles(pluginFolder);
        List<String> fileNames = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            Assert.assertTrue(
                    "Missing plugin file " + files.get(i).textValue() + " for "
                            + plugin,
                    new File(pluginFolder, files.get(i).textValue()).exists());
            fileNames.add(files.get(i).textValue());
        }
        final List<String> pluginFiles = Arrays
                .stream(pluginFolder.listFiles((dir, name) -> FilenameUtils
                        .getExtension(name).equals("js")))
                .map(file -> file.getName()).collect(Collectors.toList());
        for (String fileName : pluginFiles) {
            Assert.assertTrue(String.format(
                    "Plugin '%s' doesn't define script file '%s' in package.json files",
                    plugin, fileName), fileNames.contains(fileName));
        }
    }

    /**
     * Get the expected plugin files from package.json
     *
     * @param pluginFolder
     * @return
     * @throws IOException
     */
    private ArrayNode getPluginFiles(File pluginFolder) throws IOException {
        final JsonNode packageJson = JacksonUtils
                .readTree(FileUtils.readFileToString(
                        new File(pluginFolder, "package.json"), UTF_8));
        return (ArrayNode) packageJson.get("files");
    }

    private File getPluginFolder(String plugin) {
        final String pluginString = Paths
                .get(BUILD_DIRECTORY, PLUGIN_TARGET, plugin).toString();
        final File pluginFolder = new File(rootFolder, pluginString);

        Assert.assertTrue("Missing plugin folder for " + plugin,
                pluginFolder.exists());
        Assert.assertTrue("Missing package.json for " + plugin,
                new File(pluginFolder, "package.json").exists());
        return pluginFolder;
    }

}
