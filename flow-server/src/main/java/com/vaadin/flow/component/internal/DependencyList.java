/*
 * Copyright 2000-2018 Vaadin Ltd.
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
        final String dependencyUrl = dependency.getUrl();

        if (urlCache.contains(dependencyUrl)) {
            Optional.ofNullable(urlToLoadedDependency.get(dependencyUrl))
                    .ifPresent(currentDependency -> handleDuplicateDependency(
                            dependency, currentDependency));
        } else {
            urlCache.add(dependencyUrl);
            urlToLoadedDependency.put(dependencyUrl, dependency);
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
}
