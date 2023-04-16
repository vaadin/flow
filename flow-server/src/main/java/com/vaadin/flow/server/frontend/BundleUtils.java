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

public class BundleUtils {

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
            bundledImports.add(jsImport.replace("theme/lumo/", "src/"));
            bundledImports.add(jsImport.replace("theme/material/", "src/"));
            bundledImports.add(jsImport
                    .replace("Frontend/generated/jar-resources/", "./"));
            bundledImports.add(jsImport.replace("Frontend/", "./"));
            bundledImports.add(jsImport.replace("Frontend/", ""));
        }
        return Collections.unmodifiableSet(bundledImports);
    }

    /**
     * Loads stats.json from the classpath (from the production bundle).
     *
     * @return the stats json as a json object
     */
    private static JsonObject loadStatsJson() {
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
