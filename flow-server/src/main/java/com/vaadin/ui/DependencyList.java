/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;

/**
 * List for storing dependencies/files (JavaScript, Stylesheets) to be loaded
 * and included on the client side.
 * <p>
 * Tracks previously sent URLs and doesn't send them again.
 *
 * @author Vaadin Ltd
 */
public class DependencyList implements Serializable {

    /**
     * Contains all added URLs to be able to do fast enough duplication
     * detection.
     */
    private final Set<String> urlCache = new HashSet<>();
    private final Map<String, Dependency> urlToLoadedDependency = new LinkedHashMap<>();

    /**
     * Creates an empty dependencies list.
     */
    DependencyList() {
        this(Collections.emptyList());
    }

    /**
     * Creates a dependency list, filled with dependencies specified.
     *
     * @param dependencies
     *            dependencies to add into the list
     */
    DependencyList(Collection<Dependency> dependencies) {
        dependencies.forEach(this::add);
    }

    private Logger getLogger() {
        return Logger.getLogger(DependencyList.class.getName());
    }

    /**
     * Adds the given dependency to be loaded by the client side.
     * <p>
     * Does not send any previously sent dependencies again.
     * <p>
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     *
     * @param dependency
     *            the dependency to include on the page
     */
    public void add(Dependency dependency) {
        String dependencyUrl = dependency.getUrl();

        if (urlCache.contains(dependencyUrl)) {
            Optional.ofNullable(urlToLoadedDependency.get(dependencyUrl))
                    .ifPresent(currentDependency -> checkDuplicateDependency(
                            dependency, currentDependency));
        } else {
            urlCache.add(dependencyUrl);
            urlToLoadedDependency.put(dependencyUrl, dependency);
        }
    }

    private void checkDuplicateDependency(Dependency newDependency,
            Dependency currentDependency) {
        if (newDependency.getLoadMode() != currentDependency.getLoadMode()) {
            getLogger().log(Level.WARNING,
                    () -> String.format(
                            "Dependency with url %s was imported with two different loading strategies: %s and %s. "
                                    + "The loading strategy is changed to %s to avoid conflicts. This may impact performance.",
                            newDependency.getUrl(), newDependency.getLoadMode(),
                            currentDependency.getLoadMode(), LoadMode.EAGER));
            if (currentDependency.getLoadMode() != newDependency.getLoadMode()) {
                throw new IllegalStateException(String.format("Dependency with url %s is loaded both with %s and %s modes",
                        currentDependency.getUrl(), currentDependency.getLoadMode(), newDependency.getLoadMode()));
            }
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
