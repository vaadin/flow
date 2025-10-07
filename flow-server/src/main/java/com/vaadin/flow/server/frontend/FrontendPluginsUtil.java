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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;

import com.vaadin.flow.internal.JacksonUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for frontend build plugins.
 *
 * Unifies getting the list of available plugins.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class FrontendPluginsUtil {

    private FrontendPluginsUtil() {
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
            final JsonNode jsonFile = getJsonFile("plugins/plugins.json");
            if (jsonFile == null) {
                throw new IllegalStateException(
                        "Couldn't locate plugins/plugins.json");
            }

            final JsonNode plugins = jsonFile.get("plugins");
            List<String> pluginsToInstall = new ArrayList<>(plugins.size());
            for (int i = 0; i < plugins.size(); i++) {
                pluginsToInstall.add(plugins.get(i).textValue());
            }
            return pluginsToInstall;
        } catch (IOException ioe) {
            throw new UncheckedIOException("Couldn't load plugins/plugins.json",
                    ioe);
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
    protected static JsonNode getJsonFile(String jsonFilePath)
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
        return JacksonUtils.readTree(jsonString);
    }

    /**
     * Get URL for given resource.
     *
     * @param resource
     *            resource to get URL for
     * @return resource URL
     */
    protected static URL getResourceUrl(String resource) {
        ClassLoader cl = TaskInstallFrontendBuildPlugins.class.getClassLoader();
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
        ClassLoader cl = TaskInstallFrontendBuildPlugins.class.getClassLoader();
        return cl.getResourceAsStream(resource);
    }

}
