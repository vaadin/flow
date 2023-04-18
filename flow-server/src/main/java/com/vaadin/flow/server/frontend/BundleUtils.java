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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public final class BundleUtils {

    private BundleUtils() {
        // Util methods only
    }

    /**
     * Loads stats.json from the classpath (from the production bundle) and
     * returns the "bundleImports" part of it.
     *
     * @return the bundle imports as a set
     */
    public static Set<String> loadBundleImports() {
        JsonObject statsJson = loadStatsJson();
        if (!statsJson.hasKey("bundleImports")) {
            return null;
        }

        Set<String> bundledImports = new HashSet<>();
        JsonArray bundleImports = statsJson.getArray("bundleImports");
        for (int i = 0; i < bundleImports.length(); i++) {
            String jsImport = bundleImports.getString(i);
            bundledImports.add(jsImport);
            bundledImports.add(jsImport.replace("/theme/lumo/", "/src/"));
            bundledImports.add(jsImport.replace("/theme/material/", "/src/"));
            bundledImports.add(jsImport
                    .replaceFirst("^Frontend/generated/jar-resources/", "./"));
            bundledImports.add(jsImport
                    .replaceFirst("^Frontend/generated/jar-resources/", ""));
            bundledImports.add(jsImport.replaceFirst("^Frontend/", "./"));
            bundledImports.add(jsImport.replaceFirst("^Frontend/", ""));
        }
        return Collections.unmodifiableSet(bundledImports);
    }

    /**
     * Loads stats.json from the classpath (from the production bundle).
     *
     * @return the stats json as a json object
     */
    static JsonObject loadStatsJson() {
        URL statsUrl = BundleUtils.class.getClassLoader()
                .getResource("META-INF/VAADIN/config/stats.json");
        if (statsUrl == null) {
            getLogger().warn("No META-INF/VAADIN/config/stats.json found");
            return Json.createObject();
        }
        try {
            return Json
                    .parse(IOUtils.toString(statsUrl, StandardCharsets.UTF_8));
        } catch (IOException e) {
            getLogger().warn(
                    "Unable to parse META-INF/VAADIN/config/stats.json", e);
            return Json.createObject();
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleUtils.class);
    }
}
