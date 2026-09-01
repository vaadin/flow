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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * Converts platform versions file to internal format which doesn't contain
 * extra information.
 * <p>
 * The result contains all framework dependencies as keys and their versions as
 * value.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
class VersionsJsonConverter {

    static final String VAADIN_CORE_NPM_PACKAGE = "@vaadin/vaadin-core";
    static final String JS_VERSION = "jsVersion";
    static final String NPM_NAME = "npmName";
    static final String NPM_VERSION = "npmVersion";

    /**
     * Key for exclusions array.
     */
    public static final String EXCLUSIONS = "exclusions";
    /**
     * Key for mode in the versions file.
     */
    public static final String MODE = "mode";

    /**
     * Mode value for dependency for Lit.
     */
    public static final String MODE_LIT = "lit";

    /**
     * Mode value for dependency for React.
     */
    public static final String MODE_REACT = "react";

    /**
     * Mode value for dependency for all modes.
     */
    public static final String MODE_ALL = "all"; // same as empty string
    private static final Object VAADIN_ROUTER = "@vaadin/router";

    private final ObjectNode convertedObject;

    private boolean reactEnabled;

    private boolean excludeWebComponents;

    private Set<String> exclusions;

    private Set<String> declaredExclusions;

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VersionsJsonConverter.class);
    }

    VersionsJsonConverter(JsonNode pinnedNpmVersions, boolean reactEnabled,
            boolean excludeWebComponents) {
        this.reactEnabled = reactEnabled;
        this.excludeWebComponents = excludeWebComponents;
        exclusions = new HashSet<>();
        declaredExclusions = new HashSet<>();
        convertedObject = JacksonUtils.createObjectNode();

        collectDependencies(pinnedNpmVersions);
        excludeDependencies();
    }

    /**
     * Collect framework managed versions to enforce that the user hasn't
     * changed.
     *
     * @return flatten the platform versions Json
     */
    ObjectNode getConvertedJson() {
        return convertedObject;
    }

    /**
     * Get the exclusions set of npm package names.
     * <p>
     * Includes the packages left out because of the mode they apply to, which
     * are excluded for this versions file rather than by the file saying so.
     *
     * @return the exclusions set
     */
    Set<String> getExclusions() {
        return exclusions;
    }

    /**
     * Get the npm package names the versions file itself excludes, in its
     * {@value #EXCLUSIONS} arrays.
     *
     * @return the exclusions the versions file declares
     */
    Set<String> getDeclaredExclusions() {
        return declaredExclusions;
    }

    private void collectDependencies(JsonNode obj) {
        for (String key : JacksonUtils.getKeys(obj)) {
            JsonNode value = obj.get(key);
            if (!(value instanceof ObjectNode)) {
                continue;
            }
            if (value.has(NPM_NAME)) {
                addDependency(value);
            } else {
                collectDependencies(value);
            }
        }
    }

    private void excludeDependencies() {
        for (String key : JacksonUtils.getKeys(convertedObject)) {
            if (exclusions.contains(key)) {
                convertedObject.remove(key);
            }
        }
    }

    private boolean isIncludedByMode(String mode) {
        if (mode == null || mode.isBlank() || MODE_ALL.equalsIgnoreCase(mode)) {
            return true;
        } else if (excludeWebComponents) {
            return false;
        } else if (reactEnabled) {
            return MODE_REACT.equalsIgnoreCase(mode);
        } else {
            return MODE_LIT.equalsIgnoreCase(mode);
        }
    }

    private void addDependency(JsonNode obj) {
        assert obj.has(NPM_NAME);
        String npmName = obj.get(NPM_NAME).asString();
        String mode = obj.has(MODE) ? obj.get(MODE).asString() : null;
        String version;
        // #11025
        if (Objects.equals(npmName, VAADIN_CORE_NPM_PACKAGE)) {
            return;
        }
        if (reactEnabled && Objects.equals(npmName, VAADIN_ROUTER)) {
            exclusions.add(npmName);
            return;
        }
        if (!isIncludedByMode(mode)) {
            if (excludeWebComponents) {
                // collecting exclusions also from non-included dependencies
                // with a mode (react), when web components are not wanted.
                // The package is not installed from this file, so what it
                // excludes is not something this file says about the package
                if (MODE_REACT.equalsIgnoreCase(mode)) {
                    exclusions.add(npmName);
                }
                collectExclusions(obj, false);
            }
            return;
        }
        if (obj.has(NPM_VERSION)) {
            version = obj.get(NPM_VERSION).asString();
        } else if (obj.has(JS_VERSION)) {
            version = obj.get(JS_VERSION).asString();
        } else {
            throw new IllegalStateException("Vaadin code versions file "
                    + "contains unexpected data: dependency '" + npmName
                    + "' has" + " no 'npmVersion'/'jsVersion' . "
                    + "Please report a bug in https://github.com/vaadin/platform/issues/new");
        }
        convertedObject.put(npmName, version);

        collectExclusions(obj, true);
        getLogger().debug("versions.json adds dependency {} with version {}{}",
                npmName, version, (mode != null ? " for mode " + mode : ""));
    }

    /**
     * Collects what a dependency excludes.
     *
     * @param declared
     *            whether the dependency is one this versions file installs, so
     *            that what it excludes is what the file says about the packages
     *            rather than a consequence of the mode
     */
    private void collectExclusions(JsonNode obj, boolean declared) {
        if (obj.has(EXCLUSIONS)) {
            ArrayNode array = (ArrayNode) obj.get(EXCLUSIONS);
            if (array != null) {
                IntStream.range(0, array.size()).forEach(i -> {
                    exclusions.add(array.get(i).asString());
                    if (declared) {
                        declaredExclusions.add(array.get(i).asString());
                    }
                });
            }
        }
    }

}
