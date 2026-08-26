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
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Excludes dependencies listed in an "exclusions" array of an npm versions file
 * from a package.json.
 * 
 * @since 24.4
 */
public class ExclusionFilter implements Serializable {

    /**
     * Packages that are part of a package Flow manages the version of, mapped
     * to the package that owns them.
     * <p>
     * The owning package is released together with the packages it is built
     * from and only works with the versions it was released with, so a version
     * declared for one of them elsewhere cannot be honoured. Pinning them
     * separately resolves the owning package against packages it was never
     * combined with, which leaves the application with two implementations of
     * the same API.
     */
    private static final Map<String, String> OWNED_PACKAGES = Map.of(
            "lit-element", "lit", "lit-html", "lit", "@lit/reactive-element",
            "lit");

    private final ClassFinder finder;

    private final boolean reactEnabled;

    private final boolean excludeWebComponentNpmPackages;

    /**
     * Create a new exclusion filter.
     *
     * @param finder
     *            the class finder to use
     * @param reactEnabled
     *            whether React is enabled
     */
    public ExclusionFilter(ClassFinder finder, boolean reactEnabled) {
        this(finder, reactEnabled, false);
    }

    /**
     * Create a new exclusion filter.
     *
     * @param finder
     *            the class finder to use
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponentNpmPackages
     *            whether to exclude web component npm packages
     * @since 24.6
     */
    public ExclusionFilter(ClassFinder finder, boolean reactEnabled,
            boolean excludeWebComponentNpmPackages) {
        this.finder = finder;
        this.reactEnabled = reactEnabled;
        this.excludeWebComponentNpmPackages = excludeWebComponentNpmPackages;
    }

    /**
     * Exclude dependencies from the given map based on the npm versions files,
     * and dependencies that are part of a package Flow manages the version of.
     *
     * @param dependencies
     *            the dependencies to filter
     * @return the filtered dependencies
     * @throws IOException
     *             if an I/O error occurs
     */
    public Map<String, String> exclude(Map<String, String> dependencies)
            throws IOException {
        var exclusions = getExclusions();
        return dependencies.entrySet().stream()
                .filter(entry -> !exclusions.contains(entry.getKey()))
                .filter(entry -> !comesWithOwningPackage(entry))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    private boolean comesWithOwningPackage(
            Map.Entry<String, String> dependency) {
        String owner = OWNED_PACKAGES.get(dependency.getKey());
        if (owner == null) {
            return false;
        }
        getLogger().warn(
                """
                        Ignoring the version '{}' declared for '{}', which is part of '{}' and comes with it.
                        Declaring a version for it resolves '{}' against packages it was not released with.
                        Remove the @NpmPackage annotation for '{}' from the add-on or application declaring it.""",
                dependency.getValue(), dependency.getKey(), owner, owner,
                dependency.getKey());
        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ExclusionFilter.class);
    }

    private Set<String> getExclusions() throws IOException {
        return new NpmVersions(finder).getExclusions(reactEnabled,
                excludeWebComponentNpmPackages);
    }
}
