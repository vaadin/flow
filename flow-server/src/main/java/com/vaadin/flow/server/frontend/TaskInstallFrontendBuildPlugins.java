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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendPluginsUtil.PLUGIN_TARGET;

/**
 * Task that installs any Flow frontend plugins into node_modules/@vaadin for
 * use with frontend compilation.
 * <p>
 * Plugins are copied to <code>{build directory}/plugins</code> and linked to
 * <code>@vaadin/{plugin name}</code> in node_modules by using (p)npm install.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class TaskInstallFrontendBuildPlugins implements FallibleCommand {

    private final File targetFolder;

    /**
     * Copy Flow frontend plugins into <code>PLUGIN_TARGET</code> under the
     * build directory.
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
                        "Installation of Flow frontend plugin '" + plugin
                                + "' failed",
                        ioe);
            }
        });
    }

    private void generatePluginFiles(String pluginName) throws IOException {
        // Get the target folder where the plugin should be installed to
        File pluginTargetFolder = new File(targetFolder, pluginName);

        final String pluginFolderName = PLUGIN_TARGET + "/" + pluginName + "/";
        final JsonNode packageJson = FrontendPluginsUtil
                .getJsonFile(pluginFolderName + PACKAGE_JSON);
        if (packageJson == null) {
            log().error(
                    "Couldn't locate '{}' for plugin '{}'. Plugin will not be installed.",
                    PACKAGE_JSON, pluginName);
            return;
        }

        if (pluginTargetFolder.exists()
                && new File(pluginTargetFolder, PACKAGE_JSON).exists()) {
            String packageFile = Files.readString(
                    new File(pluginTargetFolder, PACKAGE_JSON).toPath(),
                    StandardCharsets.UTF_8);
            final JsonNode targetJson = JacksonUtils.readTree(packageFile);
            if (targetJson.has("update")
                    && !targetJson.get("update").booleanValue()) {
                // This is used only while developing the plugins inside the
                // Flow project and the attribute is then added manually to
                // package.json
                return;
            }
        }

        // Create target folder if necessary
        Files.createDirectories(pluginTargetFolder.toPath());

        // copy only files named in package.json { files }
        final JsonNode files = packageJson.get("files");
        for (int i = 0; i < files.size(); i++) {
            final String file = files.get(i).textValue();
            copyIfNeeded(new File(pluginTargetFolder, file),
                    pluginFolderName + file);
        }
        // copy package.json to plugin directory
        copyIfNeeded(new File(pluginTargetFolder, PACKAGE_JSON),
                pluginFolderName + PACKAGE_JSON);
    }

    private void copyIfNeeded(File targetFile, String sourceResource)
            throws IOException {
        String content = StringUtil.toUtf8Str(FrontendPluginsUtil
                .getResourceAsStream(sourceResource));
        FileIOUtils.writeIfChanged(targetFile, content);
    }

    private Logger log() {
        return LoggerFactory.getLogger(TaskInstallFrontendBuildPlugins.class);
    }
}
