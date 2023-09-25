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

import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;

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

    private final JsonObject userManagedDependencies;
    private final JsonObject vaadinVersions;

    private final String dependenciesKey;

    private static final String OLDER_VERSION_WARNING = "Using user (package.json) pinned version '{}' of '{}' which is older than the current platform version '{}'";

    VersionsJsonFilter(JsonObject packageJson, String dependenciesKey) {
        this.dependenciesKey = dependenciesKey;
        userManagedDependencies = collectUserManagedDependencies(packageJson);
        vaadinVersions = collectFrameworkVersions(packageJson);
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
    JsonObject getFilteredVersions(JsonObject versions, String versionOrigin) {
        JsonObject json = Json.createObject();
        for (String key : versions.keys()) {
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
                            userManagedDependencies.getString(key), key,
                            versions.getString(key));
                }
            }
            json.put(key, versions.getString(key));
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
    private JsonObject collectUserManagedDependencies(JsonObject packageJson) {
        JsonObject json = Json.createObject();
        JsonObject vaadinDep = collectFrameworkVersions(packageJson);

        if (packageJson.hasKey(dependenciesKey)) {
            JsonObject dependencies = packageJson.getObject(dependenciesKey);

            for (String key : dependencies.keys()) {
                if (isUserChanged(key, vaadinDep, dependencies)) {
                    json.put(key, dependencies.getString(key));
                }
            }
        }

        return json;
    }

    private boolean isUserChanged(String key, JsonObject vaadinDep,
            JsonObject dependencies) {
        if (vaadinDep.hasKey(key)) {
            try {
                FrontendVersion vaadin = new FrontendVersion(key,
                        vaadinDep.getString(key));
                FrontendVersion dep = new FrontendVersion(key,
                        dependencies.getString(key));
                return !vaadin.isEqualTo(dep);
            } catch (NumberFormatException nfe) {
                LoggerFactory.getLogger("VersionsFilter").debug(
                        "Received version with non numbers {} and {}",
                        vaadinDep.getString(key), dependencies.getString(key));
                return !vaadinDep.getString(key)
                        .equals(dependencies.getString(key));
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
    private JsonObject collectFrameworkVersions(JsonObject packageJson) {
        if (packageJson.hasKey(VAADIN_DEP_KEY) && packageJson
                .getObject(VAADIN_DEP_KEY).hasKey(dependenciesKey)) {
            return packageJson.getObject(VAADIN_DEP_KEY)
                    .getObject(dependenciesKey);
        }
        return Json.createObject();
    }
}
