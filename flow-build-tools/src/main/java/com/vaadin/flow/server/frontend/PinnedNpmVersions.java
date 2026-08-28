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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.server.frontend.VersionsJsonConverter.JS_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_NAME;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

/**
 * The npm packages whose versions are pinned and the versions to pin them to,
 * read from the versions files the platform ships,
 * {@value Constants#VAADIN_CORE_VERSIONS_JSON} and
 * {@value Constants#VAADIN_VERSIONS_JSON}.
 * <p>
 * The files are read in that order and a package defined in both is pinned to
 * the version of the file read last.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class PinnedNpmVersions {

    /**
     * A versions file and the location it was read from.
     */
    private record VersionsFile(String origin, JsonNode content) {
    }

    private final List<VersionsFile> files;

    /**
     * Reads the versions files visible to the given class finder.
     *
     * @param finder
     *            the class finder to look up the versions files with
     * @throws IOException
     *             if a versions file cannot be read
     */
    PinnedNpmVersions(ClassFinder finder) throws IOException {
        List<VersionsFile> versionsFiles = new ArrayList<>();
        for (String versionsFile : List.of(Constants.VAADIN_CORE_VERSIONS_JSON,
                Constants.VAADIN_VERSIONS_JSON)) {
            URL resource = finder.getResource(versionsFile);
            if (resource == null) {
                // vaadin-versions.json is only there for the commercial
                // packages, and neither file is there without the platform
                continue;
            }
            try (InputStream content = resource.openStream()) {
                versionsFiles.add(new VersionsFile(versionsFile, JacksonUtils
                        .readTree(StringUtil.toUTF8String(content))));
            }
        }
        files = List.copyOf(versionsFiles);
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
     * Gets the Vaadin version, i.e. the version of the platform the versions
     * files come from, as declared in their {@code platform} field.
     *
     * @return the Vaadin version, or empty if no versions file declares one
     */
    Optional<String> getVaadinVersion() {
        for (VersionsFile file : files) {
            JsonNode platform = file.content().get("platform");
            if (platform != null && platform.isString()) {
                return Optional.of(platform.asString());
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the npm packages and versions to pin, excluding the packages that
     * are not used in the current mode and the ones the given filter drops.
     * <p>
     * Each versions file is filtered on its own, before the files are merged,
     * so that the filter reports the file a version came from, and so that a
     * version it drops falls back to the one another file declares.
     *
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponents
     *            whether to exclude web component npm packages
     * @param filter
     *            the filter that keeps the versions the user has not changed
     * @return the npm package names and versions to pin
     */
    ObjectNode getDependencies(boolean reactEnabled,
            boolean excludeWebComponents, VersionsJsonFilter filter) {
        return merge(file -> filter.getFilteredVersions(
                new VersionsJsonConverter(file.content(), reactEnabled,
                        excludeWebComponents).getConvertedJson(),
                file.origin()));
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
     * pinned even when the packages are transitive.
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
                    // The package of the platform itself is not pinned, but
                    // the packages declared next to it still are
                    continue;
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
     * Combines the packages each versions file declares into one object, in the
     * order the files were read.
     */
    private ObjectNode merge(
            Function<VersionsFile, ObjectNode> dependenciesExtractor) {
        ObjectNode merged = JacksonUtils.createObjectNode();
        for (VersionsFile file : files) {
            ObjectNode dependencies = dependenciesExtractor.apply(file);
            for (String npmPackage : JacksonUtils.getKeys(dependencies)) {
                merged.put(npmPackage, dependencies.get(npmPackage).asString());
            }
        }
        return merged;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(PinnedNpmVersions.class);
    }
}
