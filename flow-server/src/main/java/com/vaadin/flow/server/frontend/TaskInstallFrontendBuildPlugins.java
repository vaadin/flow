/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendPluginsUtil.PLUGIN_TARGET;

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
public class TaskInstallFrontendBuildPlugins implements FallibleCommand {

    private File targetFolder;

    /**
     * Copy Flow webpack plugins into <code>PLUGIN_TARGET</code> under the build
     * directory.
     *
     * @param options
     *            the task options
     */
    public TaskInstallFrontendBuildPlugins(Options options) {
        targetFolder = new File(options.getBuildDirectory(), PLUGIN_TARGET);
    }

    @Override
    public void execute() {
        FrontendPluginsUtil.getPlugins().forEach(plugin -> {
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
        File pluginTargetFolder = new File(targetFolder, pluginName);

        final String pluginFolderName = PLUGIN_TARGET + "/" + pluginName + "/";
        final JsonObject packageJson = FrontendPluginsUtil
                .getJsonFile(pluginFolderName + PACKAGE_JSON);
        if (packageJson == null) {
            log().error(
                    "Couldn't locate '{}' for plugin '{}'. Plugin will not be installed.",
                    PACKAGE_JSON, pluginName);
            return;
        }

        if (pluginTargetFolder.exists()
                && new File(pluginTargetFolder, PACKAGE_JSON).exists()) {
            String packageFile = FileUtils.readFileToString(
                    new File(pluginTargetFolder, PACKAGE_JSON),
                    StandardCharsets.UTF_8);
            final JsonObject targetJson = Json.parse(packageFile);
            if (targetJson.hasKey("update")
                    && !targetJson.getBoolean("update")) {
                // This is used only while developing the plugins inside the
                // Flow project and the attribute is then added manually to
                // package.json
                return;
            }
        }

        // Create target folder if necessary
        FileUtils.forceMkdir(pluginTargetFolder);

        // copy only files named in package.json { files }
        final JsonArray files = packageJson.getArray("files");
        for (int i = 0; i < files.length(); i++) {
            final String file = files.getString(i);
            copyIfNeeded(new File(pluginTargetFolder, file),
                    pluginFolderName + file);
        }
        // copy package.json to plugin directory
        copyIfNeeded(new File(pluginTargetFolder, PACKAGE_JSON),
                pluginFolderName + PACKAGE_JSON);
    }

    private void copyIfNeeded(File targetFile, String sourceResource)
            throws IOException {
        String content = IOUtils.toString(
                FrontendPluginsUtil.getResourceUrl(sourceResource),
                StandardCharsets.UTF_8);
        FileIOUtils.writeIfChanged(targetFile, content);
    }

    private Logger log() {
        return LoggerFactory.getLogger(TaskInstallFrontendBuildPlugins.class);
    }
}
