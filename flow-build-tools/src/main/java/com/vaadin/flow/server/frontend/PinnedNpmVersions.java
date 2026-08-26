/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ResourceFolderUtil;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.server.frontend.VersionsJsonConverter.JS_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_NAME;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

/**
 * The npm packages whose versions are pinned and the versions to pin them to,
 * collected from all the {@value Constants#PINNED_NPM_VERSIONS_FOLDER} folders
 * on the classpath.
 * <p>
 * Each jar pinning npm package versions, such as the platform itself and the
 * component integrations, ships one or more json files in that folder. All of
 * them are read and merged into one set of packages. A package defined in
 * several files is expected to have the same version everywhere; if it does
 * not, the newest version wins and a warning is logged.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class PinnedNpmVersions {

    private static final String JSON_SUFFIX = ".json";

    /**
     * A versions file and the location it was read from.
     */
    private record VersionsFile(String origin, JsonNode content) {
    }

    private final List<VersionsFile> files;

    /**
     * Reads all versions files visible to the given class finder.
     *
     * @param finder
     *            the class finder to look up the versions files with
     * @throws IOException
     *             if a versions file cannot be read
     */
    PinnedNpmVersions(ClassFinder finder) throws IOException {
        // Sorted by origin for a deterministic order and to skip the
        // duplicates a class loader hierarchy may report for the same folder
        Map<String, VersionsFile> versionsFiles = new TreeMap<>();
        for (URL folder : finder
                .getResources(Constants.PINNED_NPM_VERSIONS_FOLDER)) {
            readVersionsFiles(folder, versionsFiles);
        }
        files = List.copyOf(versionsFiles.values());
    }

    /**
     * Reads the versions files from one folder on the classpath.
     * <p>
     * A folder that cannot be read is skipped with a warning, as the versions
     * files of the other folders still pin the packages they define.
     */
    private static void readVersionsFiles(URL folder,
            Map<String, VersionsFile> versionsFiles) {
        try {
            ResourceFolderUtil.visitFiles(folder, file -> {
                if (!file.getName().endsWith(JSON_SUFFIX)) {
                    return;
                }
                try (InputStream content = file.open()) {
                    versionsFiles.put(file.getLocation(),
                            new VersionsFile(file.getLocation(),
                                    JacksonUtils.readTree(
                                            StringUtil.toUTF8String(content))));
                }
            });
        } catch (IOException e) {
            log().warn("Unable to read pinned npm versions from '{}'."
                    + " Dependencies defined there won't be pinned for npm/pnpm/bun.",
                    folder, e);
        }
    }

    /**
     * Checks whether any versions file was found.
     *
     * @return {@code true} if no versions file is available
     */
    boolean isEmpty() {
        return files.isEmpty();
    }

    /**
     * Gets the platform version, i.e. the version of the Vaadin platform the
     * versions files come from.
     *
     * @return the platform version, or empty if no versions file declares one
     */
    Optional<String> getPlatformVersion() {
        String platformVersion = null;
        String origin = null;
        for (VersionsFile file : files) {
            JsonNode platform = file.content().get("platform");
            if (platform == null || !platform.isString()) {
                continue;
            }
            if (platformVersion == null) {
                platformVersion = platform.asString();
                origin = file.origin();
            } else if (!platformVersion.equals(platform.asString())) {
                log().warn(
                        "Conflicting platform versions found: '{}' in {} and '{}' in {}. Using '{}'.",
                        platformVersion, origin, platform.asString(),
                        file.origin(), platformVersion);
            }
        }
        return Optional.ofNullable(platformVersion);
    }

    /**
     * Gets the npm packages and versions to pin, excluding the packages that
     * are not used in the current mode and the ones excluded by another
     * package.
     *
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponents
     *            whether to exclude web component npm packages
     * @return the npm package names and versions to pin
     */
    ObjectNode getDependencies(boolean reactEnabled,
            boolean excludeWebComponents) {
        ObjectNode dependencies = merge(
                file -> new VersionsJsonConverter(file.content(), reactEnabled,
                        excludeWebComponents).getConvertedJson());
        // A package excluded in one file is excluded from all of them
        getExclusions(reactEnabled, excludeWebComponents)
                .forEach(dependencies::remove);
        return dependencies;
    }

    /**
     * Gets the npm packages that the versions files exclude.
     *
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponents
     *            whether to exclude web component npm packages
     * @return the excluded npm package names
     */
    Set<String> getExclusions(boolean reactEnabled,
            boolean excludeWebComponents) {
        Set<String> exclusions = new TreeSet<>();
        files.forEach(file -> exclusions
                .addAll(new VersionsJsonConverter(file.content(), reactEnabled,
                        excludeWebComponents).getExclusions()));
        return exclusions;
    }

    /**
     * Gets all npm packages and versions the versions files declare, regardless
     * of the mode they apply to, to use in overrides so that the versions get
     * locked even when the packages are transitive.
     *
     * @return all npm package names and versions
     */
    ObjectNode getAllDependencies() {
        return merge(file -> {
            ObjectNode dependencies = JacksonUtils.createObjectNode();
            collectDependencies(file.content(), dependencies);
            return dependencies;
        });
    }

    private static void collectDependencies(JsonNode obj,
            ObjectNode collection) {
        for (String key : JacksonUtils.getKeys(obj)) {
            JsonNode value = obj.get(key);
            if (!(value instanceof ObjectNode)) {
                continue;
            }
            if (value.has(NPM_NAME)) {
                String npmName = value.get(NPM_NAME).asString();
                if (Objects.equals(npmName, VAADIN_CORE_NPM_PACKAGE)) {
                    return;
                }
                String version;
                if (value.has(NPM_VERSION)) {
                    version = value.get(NPM_VERSION).asString();
                } else if (value.has(JS_VERSION)) {
                    version = value.get(JS_VERSION).asString();
                } else {
                    log().debug(
                            "dependency '{}' has no 'npmVersion'/'jsVersion'.",
                            npmName);
                    continue;
                }
                collection.put(npmName, version);
            } else {
                collectDependencies(value, collection);
            }
        }
    }

    /**
     * Combines the packages each versions file declares into one object, using
     * the newest version of a package declared in several files.
     */
    private ObjectNode merge(
            Function<VersionsFile, ObjectNode> dependenciesExtractor) {
        ObjectNode merged = JacksonUtils.createObjectNode();
        Map<String, String> origins = new HashMap<>();
        for (VersionsFile file : files) {
            ObjectNode dependencies = dependenciesExtractor.apply(file);
            for (String npmPackage : JacksonUtils.getKeys(dependencies)) {
                String version = dependencies.get(npmPackage).asString();
                if (merged.has(npmPackage) && !acceptsVersion(merged, origins,
                        npmPackage, version, file.origin())) {
                    continue;
                }
                merged.put(npmPackage, version);
                origins.put(npmPackage, file.origin());
            }
        }
        return merged;
    }

    /**
     * Resolves a package declared in several versions files, keeping the newest
     * version and warning about the conflict.
     *
     * @return {@code true} if the new version should replace the current one
     */
    private boolean acceptsVersion(ObjectNode merged,
            Map<String, String> origins, String npmPackage, String version,
            String origin) {
        String currentVersion = merged.get(npmPackage).asString();
        if (currentVersion.equals(version)) {
            return false;
        }
        boolean newer = isNewerThan(version, currentVersion);
        log().warn(
                "Multiple versions defined for npm package '{}': '{}' in {} and '{}' in {}. Using '{}'.",
                npmPackage, currentVersion, origins.get(npmPackage), version,
                origin, newer ? version : currentVersion);
        return newer;
    }

    private static boolean isNewerThan(String version, String otherVersion) {
        try {
            return new FrontendVersion(version)
                    .isNewerThan(new FrontendVersion(otherVersion));
        } catch (NumberFormatException e) {
            log().debug("Unable to compare versions '{}' and '{}'", version,
                    otherVersion, e);
            return false;
        }
    }

    private static Logger log() {
        return LoggerFactory.getLogger(PinnedNpmVersions.class);
    }
}
