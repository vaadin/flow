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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * List for storing dependencies/files (JavaScript, Stylesheets) to be loaded
 * and included on the client side.
 * <p>
 * Tracks previously sent URLs and doesn't send them again.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DependencyList implements Serializable {

    /**
     * Contains all added URLs to be able to do fast enough duplication
     * detection.
     */
    private final Set<String> urlCache = new HashSet<>();
    private final Map<String, Dependency> urlToLoadedDependency = new LinkedHashMap<>();
    private final Map<String, String> dependencyIdToUrl = new HashMap<>();
    private final Map<String, String> urlToDependencyId = new HashMap<>();

    /**
     * Creates a new instance.
     */
    public DependencyList() {
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(DependencyList.class.getName());
    }

    /**
     * Adds the given dependency to be loaded by the client side.
     * <p>
     * Does not send any previously sent dependencies again.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     *
     * @param dependency
     *            the dependency to include on the page
     */
    public void add(Dependency dependency) {
        add(dependency, null);
    }

    /**
     * Adds the given dependency to be loaded by the client side with an
     * optional ID.
     * <p>
     * Does not send any previously sent dependencies again.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param dependency
     *            the dependency to include on the page
     * @param dependencyId
     *            optional ID for tracking the dependency
     */
    public void add(Dependency dependency, String dependencyId) {
        final String dependencyUrl = dependency.getUrl();

        if (urlCache.contains(dependencyUrl)) {
            Optional.ofNullable(urlToLoadedDependency.get(dependencyUrl))
                    .ifPresent(currentDependency -> handleDuplicateDependency(
                            dependency, currentDependency));
        } else {
            urlCache.add(dependencyUrl);
            urlToLoadedDependency.put(dependencyUrl, dependency);

            // Track dependency ID if provided
            if (dependencyId != null) {
                dependencyIdToUrl.put(dependencyId, dependencyUrl);
                urlToDependencyId.put(dependencyUrl, dependencyId);
            }
        }
    }

    private void handleDuplicateDependency(Dependency newDependency,
            Dependency currentDependency) {
        if (newDependency.getLoadMode() != currentDependency.getLoadMode()) {
            final LoadMode moreEagerLoadMode = LoadMode.values()[Math.min(
                    newDependency.getLoadMode().ordinal(),
                    currentDependency.getLoadMode().ordinal())];
            getLogger().warn(
                    "Dependency with url {} was imported with two different loading strategies: {} and {}. The dependency will be loaded as {}.",
                    newDependency.getUrl(), newDependency.getLoadMode(),
                    currentDependency.getLoadMode(), moreEagerLoadMode);
            urlToLoadedDependency.replace(newDependency.getUrl(),
                    new Dependency(newDependency.getType(),
                            newDependency.getUrl(), moreEagerLoadMode));
        }
    }

    /**
     * Returns a list of dependencies which should be sent to the client.
     *
     * @return a list containing the dependencies which should be sent
     */
    public Collection<Dependency> getPendingSendToClient() {
        return urlToLoadedDependency.values();
    }

    /**
     * Clears the list of dependencies which should be sent to the client.
     */
    public void clearPendingSendToClient() {
        urlToLoadedDependency.clear();
    }

    /**
     * Gets the dependency ID associated with the given URL, if any.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param url
     *            the URL to look up
     * @return the dependency ID or null if not tracked
     */
    public String getDependencyId(String url) {
        return urlToDependencyId.get(url);
    }

    /**
     * Removes a dependency by its ID.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param dependencyId
     *            the ID of the dependency to remove
     * @return true if the dependency was removed, false if it wasn't found
     */
    public boolean remove(String dependencyId) {
        String url = dependencyIdToUrl.remove(dependencyId);
        if (url != null) {
            urlToDependencyId.remove(url);
            urlToLoadedDependency.remove(url);
            urlCache.remove(url);
            return true;
        }
        return false;
    }
}
