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
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.WebpackPluginsUtil.PLUGIN_TARGET;

/**
 * Task that installs any Flow webpack plugins into node_modules/@vaadin for use
 * with webpack compilation.
 * <p>
 * Plugins are copied to <code>{build directory}/plugins</code> and linked to
 * <code>@vaadin/{plugin name}</code> in node_modules by using (p)npm install.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public class TaskInstallWebpackPlugins implements FallibleCommand {

    private File targetFolder;

    /**
     * Copy Flow webpack plugins into <code>PLUGIN_TARGET</code> under the build
     * directory.
     *
     * @param buildDirectory
     *            project build folder
     */
    public TaskInstallWebpackPlugins(File buildDirectory) {
        targetFolder = new File(buildDirectory, PLUGIN_TARGET);
    }

    @Override
    public void execute() {
        WebpackPluginsUtil.getPlugins().forEach(plugin -> {
            try {
                generatePluginFiles(plugin);
            } catch (IOException ioe) {
                throw new UncheckedIOException(
                        "Installation of Flow webpack plugin '" + plugin
                                + "' failed",
                        ioe);
            }
        });
    }

    private void generatePluginFiles(String pluginName) throws IOException {
        // Get the target folder where the plugin should be installed to
        File pluginTargetFile = new File(targetFolder, pluginName);

        final String pluginFolderName = PLUGIN_TARGET + "/" + pluginName + "/";
        final JsonObject packageJson = WebpackPluginsUtil
                .getJsonFile(pluginFolderName + PACKAGE_JSON);
        if (packageJson == null) {
            log().error(
                    "Couldn't locate '{}' for plugin '{}'. Plugin will not be installed.",
                    PACKAGE_JSON, pluginName);
            return;
        }

        // Validate installed version and don't override if same
        if (pluginTargetFile.exists()
                && new File(pluginTargetFile, PACKAGE_JSON).exists()) {
            String packageFile = FileUtils.readFileToString(
                    new File(pluginTargetFile, PACKAGE_JSON),
                    StandardCharsets.UTF_8);
            final JsonObject targetJson = Json.parse(packageFile);
            if (targetJson.hasKey("update")
                    && !targetJson.getBoolean("update")) {
                return;
            }
        }

        // Create target folder if necessary
        FileUtils.forceMkdir(pluginTargetFile);

        // copy only files named in package.json { files }
        final JsonArray files = packageJson.getArray("files");
        for (int i = 0; i < files.length(); i++) {
            final String file = files.getString(i);
            FileUtils.copyURLToFile(
                    WebpackPluginsUtil.getResourceUrl(pluginFolderName + file),
                    new File(pluginTargetFile, file));
        }
        // copy package.json to plugin directory
        FileUtils.copyURLToFile(
                WebpackPluginsUtil
                        .getResourceUrl(pluginFolderName + PACKAGE_JSON),
                new File(pluginTargetFile, PACKAGE_JSON));
    }

    private Logger log() {
        return LoggerFactory.getLogger(TaskInstallWebpackPlugins.class);
    }
}
