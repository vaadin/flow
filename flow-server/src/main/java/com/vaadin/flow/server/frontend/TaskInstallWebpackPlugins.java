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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task that installs any Flow webpack plugins into node_modules/@vaadin for use
 * with webpack compilation.
 * <p>
 * This should preferably be executed after npm installation to not make it skip
 * or have the plugins deleted by {@link TaskRunNpmInstall}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public class TaskInstallWebpackPlugins implements FallibleCommand {

    private File nodeModulesFolder;

    /**
     * Copy Flow webpack plugins into the given nodeModulesFolder.
     *
     * @param nodeModulesFolder
     *            node_modules folder to copy files to
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
                                + "' failed",
                        ioe);
            }
        });
    }

    /**
     * Get names for plugins to install into node_modules.
     *
     * @return names of plugins to install
     */
    protected List<String> getPlugins() {
        try {
            final JsonObject jsonFile = getJsonFile(
                    "plugins/webpack-plugins.json");
            if (jsonFile == null) {
                log().error(
                        "Couldn't locate plugins/webpack-plugins.json, no Webpack plugins for Flow will be installed."
                                + "If webpack build fails validate flow-server jar content.");
                return Collections.emptyList();
            }

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
        File pluginTargetFile = new File(nodeModulesFolder,
                "@vaadin/" + pluginName);

        final String pluginFolderName = "plugins/" + pluginName + "/";
        final JsonObject packageJson = getJsonFile(
                pluginFolderName + PACKAGE_JSON);
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
            FileUtils.copyURLToFile(getResourceUrl(pluginFolderName + file),
                    new File(pluginTargetFile, file));
        }
        // copy package.json to plugin directory
        FileUtils.copyURLToFile(getResourceUrl(pluginFolderName + PACKAGE_JSON),
                new File(pluginTargetFile, PACKAGE_JSON));
    }

    private JsonObject getJsonFile(String jsonFilePath) throws IOException {
        final URL urlResource = getResourceUrl(jsonFilePath);
        if (urlResource == null) {
            return null;
        }
        File jsonFile = new File(urlResource.getFile());
        String jsonString;
        if (!jsonFile.exists()) {
            try (InputStream resourceAsStream = this.getClass().getClassLoader()
                    .getResourceAsStream(jsonFilePath)) {
                if (resourceAsStream != null) {
                    jsonString = FrontendUtils.streamToString(resourceAsStream);
                } else {
                    return null;
                }
            }
        } else {
            jsonString = FileUtils.readFileToString(jsonFile, UTF_8);
        }
        return Json.parse(jsonString);
    }

    private URL getResourceUrl(String resource) {
        return this.getClass().getClassLoader().getResource(resource);
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
