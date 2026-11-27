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

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.InitParameters;

import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;

/**
 * Filters the versions to pin, read from a versions file, down to the ones that
 * can be pinned: numeric versions, without SNAPSHOT builds, and URL versions
 * when exotic versions are allowed. Any other exotic version is left out with a
 * warning that names the opt-in. When the user has changed the version of a
 * package in package.json to an older one than the versions file declares, a
 * warning is logged; the version the user declared is respected through the
 * overrides, not by leaving it out here.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
class VersionsJsonFilter {

    private final ObjectNode userManagedDependencies;

    private final String dependenciesKey;

    private final boolean allowExoticVersions;

    private static final String OLDER_VERSION_WARNING = "Using user (package.json) pinned version '{}' of '{}' which is older than the current platform version '{}'";

    private static final String EXOTIC_VERSION_WARNING = "Ignoring the version '{}' of npm package '{}' declared in {},"
            + " as versions that are neither a registry version nor a local path"
            + " are not allowed. Set the 'npmAllowExoticVersions' plugin parameter"
            + " or the '{}' configuration property to true to allow them.";

    /**
     * Creates a filter for the given package.json.
     *
     * @param packageJson
     *            the package.json content, used to find the versions the user
     *            changed
     * @param dependenciesKey
     *            the package.json section to compare with, e.g.
     *            {@code dependencies}
     * @param allowExoticVersions
     *            whether a version that is neither a registry version nor a
     *            local path, such as a tarball URL, may be pinned
     */
    VersionsJsonFilter(ObjectNode packageJson, String dependenciesKey,
            boolean allowExoticVersions) {
        this.dependenciesKey = dependenciesKey;
        this.allowExoticVersions = allowExoticVersions;
        userManagedDependencies = collectUserManagedDependencies(packageJson);
    }

    /**
     * Gets the versions that can be pinned, warning about the packages the user
     * has pinned to an older version in package.json. An exotic version is left
     * out with a warning unless exotic versions are allowed, and then only a
     * URL version is kept.
     *
     * @param versions
     *            to be filtered for user managed ones
     * @param versionOrigin
     *            origin of the version (like a file), used in warnings and
     *            error messages
     * @return filtered versions json
     */
    ObjectNode getFilteredVersions(ObjectNode versions, String versionOrigin) {
        ObjectNode json = JacksonUtils.createObjectNode();
        for (String key : JacksonUtils.getKeys(versions)) {
            final String value = versions.get(key).asString(null);
            if (FrontendBuildUtils.isExoticVersion(value)) {
                if (!allowExoticVersions) {
                    LoggerFactory.getLogger(VersionsJsonFilter.class).warn(
                            EXOTIC_VERSION_WARNING, value, key, versionOrigin,
                            InitParameters.NPM_ALLOW_EXOTIC_VERSIONS);
                    continue;
                }
                if (FrontendBuildUtils.isUrlVersion(value)) {
                    // a URL cannot be compared with the version of the user,
                    // so it is used as is
                    json.put(key, value);
                    continue;
                }
            }
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
                            userManagedDependencies.get(key).asString(), key,
                            versions.get(key).asString());
                }
            }
            json.put(key, versions.get(key).asString());
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
                    json.put(key, dependencies.get(key).asString());
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
                        vaadinDep.get(key).asString());
                FrontendVersion dep = new FrontendVersion(key,
                        dependencies.get(key).asString());
                return !vaadin.isSameDependency(dep);
            } catch (NumberFormatException nfe) {
                LoggerFactory.getLogger("VersionsFilter").debug(
                        "Received version with non numbers {} and {}",
                        vaadinDep.get(key).asString(),
                        dependencies.get(key).asString());
                return !vaadinDep.get(key).asString()
                        .equals(dependencies.get(key).asString());
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
