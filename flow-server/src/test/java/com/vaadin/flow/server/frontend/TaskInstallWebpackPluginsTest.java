/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TaskInstallWebpackPluginsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File nodeModulesFolder;

    private TaskInstallWebpackPlugins task;

    @Before
    public void init() throws IOException {
        nodeModulesFolder = temporaryFolder.newFolder();
        task = new TaskInstallWebpackPlugins(nodeModulesFolder);
    }

    @Test
    public void getPluginsReturnsExpectedList() {
        String[] expectedPlugins = new String[] { "stats-plugin",
                "application-theme-plugin", "theme-loader",
                "theme-live-reload-plugin" };
        final List<String> plugins = task.getPlugins();
        Assert.assertEquals(
                "Unexpected amount of plugins in 'webpack-plugins.json'",
                expectedPlugins.length, plugins.size());

        for (String plugin : expectedPlugins) {
            Assert.assertTrue(
                    "'webpack-plugins.json' didn't contain '" + plugin + "'",
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
        for (String plugin : task.getPlugins()) {
            verifyPluginScriptFilesAreDefined(plugin);
        }
    }

    private void assertPlugins() throws IOException {
        Assert.assertTrue("No @vaadin folder created",
                new File(nodeModulesFolder, "@vaadin").exists());
        for (String plugin : task.getPlugins()) {
            assertPlugin(plugin);
        }
    }

    private void assertPlugin(String plugin) throws IOException {
        final File pluginFolder = getPluginFolder(plugin);

        final JsonArray files = getPluginFiles(pluginFolder);
        for (int i = 0; i < files.length(); i++) {
            Assert.assertTrue(
                    "Missing plugin file " + files.getString(i) + " for "
                            + plugin,
                    new File(pluginFolder, files.getString(i)).exists());
        }
    }

    private void verifyPluginScriptFilesAreDefined(String plugin)
            throws IOException {
        final File pluginFolder = new File(this.getClass().getClassLoader()
                .getResource("plugins/" + plugin).getFile());

        final JsonArray files = getPluginFiles(pluginFolder);
        List<String> fileNames = new ArrayList<>(files.length());
        for (int i = 0; i < files.length(); i++) {
            Assert.assertTrue(
                    "Missing plugin file " + files.getString(i) + " for "
                            + plugin,
                    new File(pluginFolder, files.getString(i)).exists());
            fileNames.add(files.getString(i));
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
    private JsonArray getPluginFiles(File pluginFolder) throws IOException {
        final JsonObject packageJson = Json.parse(FileUtils.readFileToString(
                new File(pluginFolder, "package.json"), UTF_8));
        return packageJson.getArray("files");
    }

    private File getPluginFolder(String plugin) {
        final String pluginString = "@vaadin" + File.separator + plugin;
        final File pluginFolder = new File(nodeModulesFolder, pluginString);

        Assert.assertTrue("Missing plugin folder for " + plugin,
                pluginFolder.exists());
        Assert.assertTrue("Missing package.json for " + plugin,
                new File(pluginFolder, "package.json").exists());
        return pluginFolder;
    }

}
