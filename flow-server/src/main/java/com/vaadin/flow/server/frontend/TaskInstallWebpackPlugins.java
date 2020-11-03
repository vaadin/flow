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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task that installs any Flow webpack plugins into node_modules/@vaadin for
 * use with webpack compilation.
 * <p>
 * This should preferably be executed after npm installation to not make it skip
 * or have the plugins deleted by {@link TaskRunNpmInstall}.
 *
 * @since
 */
public class TaskInstallWebpackPlugins implements FallibleCommand {

    private File nodeModulesFolder;

    /**
     * Copy Flow webpack plugins into the given nodeModulesFolder.
     *
     * @param nodeModulesFolder
     *     node_modules folder to copy files to
     */
    public TaskInstallWebpackPlugins(File nodeModulesFolder) {
        this.nodeModulesFolder = nodeModulesFolder;
    }

    @Override
    public void execute() {
        getPlugins().forEach(plugin -> {
            try {
                generatePluginFiles(plugin);
            } catch (IOException ioe) {
                throw new UncheckedIOException(
                    "Installation of Flow webpack plugin '" + plugin
                        + "' failed", ioe);
            }
        });
    }

    protected List<String> getPlugins() {
        try {
            final JsonObject jsonFile = getJsonFile("plugins/",
                "webpack-plugins.json");
            final JsonArray plugins = jsonFile.getArray("plugins");
            List<String> pluginsToInstall = new ArrayList<>(plugins.length());
            for (int i = 0; i < plugins.length(); i++) {
                pluginsToInstall.add(plugins.getString(i));
            }
            return pluginsToInstall;
        } catch (IOException ioe) {
            throw new UncheckedIOException(
                "Couldn't load webpack-plugins.json file", ioe);
        }
    }

    private void generatePluginFiles(String pluginName) throws IOException {
        // Get the target folder where the plugin should be installed to
        File pluginTargetFolder = new File(nodeModulesFolder,
            "@vaadin/" + pluginName);

        final String pluginFolder = "plugins/" + pluginName;
        final JsonObject json = getJsonFile(pluginFolder, "package.json");
        if (json == null) {
            log().error("Couldn't locate files for plugin '{}'", pluginName);
            return;
        }

        // Validate installed version and don't override if same
        if (pluginTargetFolder.exists() && new File(pluginTargetFolder,
            "package.json").exists()) {
            String packageFile = FileUtils
                .readFileToString(new File(pluginTargetFolder, "package.json"),
                    StandardCharsets.UTF_8);
            final FrontendVersion packageVersion = new FrontendVersion(
                Json.parse(packageFile).getString("version"));
            FrontendVersion pluginVersion = new FrontendVersion(
                json.getString("version"));
            if (packageVersion.isEqualTo(pluginVersion)) {
                log().debug(
                    "Skipping install of {} for version {} already installed",
                    pluginName, pluginVersion.getFullVersion());
                return;
            }
        }

        // Create target folder if necessary
        FileUtils.forceMkdir(pluginTargetFolder);

        // copy only files named in package.json { files }
        final JsonArray files = json.getArray("files");
        for (int i = 0; i < files.length(); i++) {
            final String string = files.getString(i);
            FileUtils.copyURLToFile(getUrlResource(pluginFolder + "/" + string),
                new File(pluginTargetFolder, string));
        }
        // copy package.json to plugin directory
        FileUtils.copyURLToFile(getUrlResource(pluginFolder + "/package.json"),
            new File(pluginTargetFolder, "package.json"));
    }

    private JsonObject getJsonFile(String pluginFolder, String fileName)
        throws IOException {
        File pluginPackageJson = new File(
            getUrlResource(pluginFolder + "/" + fileName).getFile());
        String jsonString;
        if (!pluginPackageJson.exists()) {
            try (InputStream resourceAsStream = this.getClass().getClassLoader()
                .getResourceAsStream(pluginFolder + "/" + fileName)) {
                if (resourceAsStream != null) {
                    jsonString = FrontendUtils.streamToString(resourceAsStream);
                } else {
                    return null;
                }
            }
        } else {
            jsonString = FileUtils.readFileToString(pluginPackageJson, UTF_8);
        }
        return Json.parse(jsonString);
    }

    private URL getUrlResource(String resource) {
        return this.getClass().getClassLoader().getResource(resource);
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
