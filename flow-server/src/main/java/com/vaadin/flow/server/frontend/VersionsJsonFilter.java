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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.JacksonUtils;

import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;

/**
 * Filters out versions based on package json (if user has defined a custom
 * version then this version is not included into the result).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
class VersionsJsonFilter {

    private final ObjectNode userManagedDependencies;

    private final String dependenciesKey;

    private static final String OLDER_VERSION_WARNING = "Using user (package.json) pinned version '{}' of '{}' which is older than the current platform version '{}'";

    VersionsJsonFilter(ObjectNode packageJson, String dependenciesKey) {
        this.dependenciesKey = dependenciesKey;
        userManagedDependencies = collectUserManagedDependencies(packageJson);
    }

    /**
     * Collect framework managed versions to enforce that the user hasn't
     * changed.
     *
     * @param versions
     *            to be filtered for user managed ones
     * @param versionOrigin
     *            origin of the version (like a file), used in error message
     * @return filtered versions json
     */
    ObjectNode getFilteredVersions(ObjectNode versions, String versionOrigin) {
        ObjectNode json = JacksonUtils.createObjectNode();
        for (String key : JacksonUtils.getKeys(versions)) {
            final FrontendVersion version = FrontendUtils
                    .getPackageVersionFromJson(versions, key, versionOrigin);
            if (version == null) {
                continue;
            }
            // for platform snapshots, snapshot is set to be the vaadin-core
            // version
            if (version.getFullVersion().contains("SNAPSHOT")) {
                continue;
            }
            final FrontendVersion userManagedVersion = FrontendUtils
                    .getPackageVersionFromJson(userManagedDependencies, key,
                            "/package.json -> { dependencies }");
            if (userManagedVersion != null) {
                if (version.isNewerThan(userManagedVersion)) {
                    LoggerFactory.getLogger("Versions").warn(
                            OLDER_VERSION_WARNING,
                            userManagedDependencies.get(key).textValue(), key,
                            versions.get(key).textValue());
                }
            }
            json.put(key, versions.get(key).textValue());
        }
        return json;
    }

    /**
     * Collect all dependencies that the user has changed that do not match the
     * flow managed dependency versions.
     *
     * @param packageJson
     *            package.json Json object
     * @return collection of user managed dependencies
     */
    private ObjectNode collectUserManagedDependencies(ObjectNode packageJson) {
        ObjectNode json = JacksonUtils.createObjectNode();
        JsonNode vaadinDep = collectFrameworkVersions(packageJson);

        if (packageJson.has(dependenciesKey)) {
            JsonNode dependencies = packageJson.get(dependenciesKey);

            for (String key : JacksonUtils.getKeys(dependencies)) {
                if (isUserChanged(key, vaadinDep, dependencies)) {
                    json.put(key, dependencies.get(key).textValue());
                }
            }
        }

        return json;
    }

    private boolean isUserChanged(String key, JsonNode vaadinDep,
            JsonNode dependencies) {
        if (vaadinDep.has(key)) {
            try {
                FrontendVersion vaadin = new FrontendVersion(key,
                        vaadinDep.get(key).textValue());
                FrontendVersion dep = new FrontendVersion(key,
                        dependencies.get(key).textValue());
                return !vaadin.isEqualTo(dep);
            } catch (NumberFormatException nfe) {
                LoggerFactory.getLogger("VersionsFilter").debug(
                        "Received version with non numbers {} and {}",
                        vaadinDep.get(key).textValue(),
                        dependencies.get(key).textValue());
                return !vaadinDep.get(key).textValue()
                        .equals(dependencies.get(key).textValue());
            }
        }
        // User changed if not in vaadin dependency
        return true;
    }

    /**
     * Get the Vaadin dependency.
     *
     * @param packageJson
     *            main package.json
     * @return Vaadin dependencies or empty object
     */
    private JsonNode collectFrameworkVersions(ObjectNode packageJson) {
        if (packageJson.has(VAADIN_DEP_KEY)
                && packageJson.get(VAADIN_DEP_KEY).has(dependenciesKey)) {
            return packageJson.get(VAADIN_DEP_KEY).get(dependenciesKey);
        }
        return JacksonUtils.createObjectNode();
    }
}
