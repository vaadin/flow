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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

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

    /**
     * Gets the id for a chunk defined by the given class.
     *
     * @param componentClass
     *            the class
     * @return an identifier for the component class that does not reveal the
     *         name of the component class
     */
    public static String getChunkId(Class<? extends Component> componentClass) {
        return getChunkId(componentClass.getName());
    }

    /**
     * Gets the id for a chunk defined by the given class.
     *
     * @param className
     *            the name of the class
     * @return an identifier for the component class that does not reveal the
     *         name of the component class
     */
    public static String getChunkId(String className) {
        return StringUtil.getHash(className, StandardCharsets.UTF_8);
    }

    /**
     * Calculates a hash for bundle JavaScript chunk containing given string
     * lines.
     *
     * @param chunkLines
     *            content of the chunk, collection of string lines
     * @return chunk's hash
     */
    public static String getChunkHash(List<String> chunkLines) {
        List<String> sortedChunkLines = new ArrayList<>(chunkLines);
        Collections.sort(sortedChunkLines);
        return StringUtil.getHash(String.join(";", sortedChunkLines),
                StandardCharsets.UTF_8);
    }

    /**
     * Returns whether the application uses pre-compiled production bundle or a
     * custom bundle.
     *
     * @return <code>true</code> in case of pre-compiled bundle,
     *         <code>false</code> otherwise
     */
    public static boolean isPreCompiledProductionBundle() {
        JsonObject stats = loadStatsJson();
        return stats.hasKey("pre-compiled");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleUtils.class);
    }

    /**
     * Copy package-lock.json/.yaml file from existing dev-bundle for building
     * new bundle.
     *
     * @param options
     *            task options
     */
    public static void copyPackageLockFromBundle(Options options) {
        String lockFile;
        if (options.isEnablePnpm()) {
            lockFile = Constants.PACKAGE_LOCK_YAML;
        } else {
            lockFile = Constants.PACKAGE_LOCK_JSON;
        }
        File packageLock = new File(options.getNpmFolder(), lockFile);
        if (packageLock.exists()) {
            // NO-OP due to existing package-lock
            return;
        }

        try {
            copyAppropriatePackageLock(options, packageLock);
        } catch (IOException ioe) {
            getLogger().error(
                    "Failed to copy existing `" + lockFile + "` to use", ioe);
        }

    }

    private static void copyAppropriatePackageLock(Options options,
            File packageLock) throws IOException {
        File devBundleFolder = new File(options.getNpmFolder(),
                Constants.DEV_BUNDLE_LOCATION);
        String packageLockFile = options.isEnablePnpm()
                ? Constants.PACKAGE_LOCK_YAML
                : Constants.PACKAGE_LOCK_JSON;
        if (devBundleFolder.exists()) {
            File devPackageLock = new File(devBundleFolder, packageLockFile);
            if (devPackageLock.exists()) {
                FileUtils.copyFile(devPackageLock, packageLock);
                return;
            }
        }
        final URL resource = options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + packageLockFile);
        if (resource != null) {
            FileUtils.write(packageLock,
                    IOUtils.toString(resource, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        }
    }
}
