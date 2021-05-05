/*
 * Copyright 2000-2020 Vaadin Ltd.
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

    private final String OLDER_VERSION_WARNING = "Using user pinned version '{}' of '{}' which is older than the current platform version '{}'";

    VersionsJsonFilter(JsonObject packageJson, String dependenciesKey) {
        this.dependenciesKey = dependenciesKey;
        userManagedDependencies = collectUserManagedDependencies(packageJson);
        vaadinVersions = collectFrameworkVersions(packageJson);
    }

    /**
     * Collect framework managed versions to enforce that the user hasn't
     * changed.
     *
     * @return filtered versions json
     */
    JsonObject getFilteredVersions(JsonObject versions) {
        JsonObject json = Json.createObject();
        for (String key : versions.keys()) {
            if (userManagedDependencies.hasKey(key)) {
                if (isNewer(versions.getString(key),
                        userManagedDependencies.getString(key))) {
                    LoggerFactory.getLogger("Versions").warn(
                            OLDER_VERSION_WARNING,
                            userManagedDependencies.getString(key), key,
                            versions.getString(key));
                }
                json.put(key, userManagedDependencies.getString(key));
            } else if (vaadinVersions.hasKey(key) && isNewer(
                    vaadinVersions.getString(key), versions.getString(key))) {
                json.put(key, vaadinVersions.getString(key));
            } else {
                json.put(key, versions.getString(key));
            }
        }
        return json;
    }

    /**
     * Check if version1 is newer than version2.
     * 
     * @param version1
     *            version string to see if newer
     * @param version2
     *            version string to compare against
     * @return true if version1 > version2
     */
    private boolean isNewer(String version1, String version2) {
        final FrontendVersion frontendVersion = new FrontendVersion(version1);
        final FrontendVersion frontendVersion2 = new FrontendVersion(version2);

        return frontendVersion.isNewerThan(frontendVersion2);
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
            FrontendVersion vaadin = new FrontendVersion(
                    vaadinDep.getString(key));
            FrontendVersion dep = new FrontendVersion(
                    dependencies.getString(key));
            return !vaadin.isEqualTo(dep);
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
