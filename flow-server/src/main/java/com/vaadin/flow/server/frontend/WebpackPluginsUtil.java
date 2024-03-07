/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for Flow Webpack plugins.
 *
 * Unifies getting the list of available plugins.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class WebpackPluginsUtil {

    private WebpackPluginsUtil() {
    }

    /**
     * Target folder for plugins.
     */
    protected static final String PLUGIN_TARGET = "plugins";

    /**
     * Get names for plugins to install into node_modules.
     *
     * @return names of plugins to install
     */
    protected static List<String> getPlugins() {
        try {
            final JsonObject jsonFile = getJsonFile(
                    "plugins/webpack-plugins.json");
            if (jsonFile == null) {
                LoggerFactory.getLogger("WebpackPlugins").error(
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

    /**
     * Load and parse the requested Json file.
     *
     * @param jsonFilePath
     *            path to json file
     * @return parsed Json for file if found
     * @throws IOException
     *             thrown for problems reading file
     */
    protected static JsonObject getJsonFile(String jsonFilePath)
            throws IOException {
        final URL urlResource = getResourceUrl(jsonFilePath);
        if (urlResource == null) {
            return null;
        }
        File jsonFile = new File(urlResource.getFile());
        String jsonString;
        if (!jsonFile.exists()) {
            try (InputStream resourceAsStream = getResourceAsStream(
                    jsonFilePath)) {
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

    /**
     * Get URL for given resource.
     *
     * @param resource
     *            resource to get URL for
     * @return resource URL
     */
    protected static URL getResourceUrl(String resource) {
        ClassLoader cl = TaskInstallWebpackPlugins.class.getClassLoader();
        return cl.getResource(resource);
    }

    /**
     * Get stream for resource.
     *
     * @param resource
     *            resource stream
     * @return input stream for resource
     */
    protected static InputStream getResourceAsStream(String resource) {
        ClassLoader cl = TaskInstallWebpackPlugins.class.getClassLoader();
        return cl.getResourceAsStream(resource);
    }

}
