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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

public interface RouteRegistry extends Serializable {

    /**
     * Registers a set of components as navigation targets.
     *
     * @param navigationTargets
     *         set of navigation target components
     * @throws InvalidRouteConfigurationException
     *         if routing has been configured incorrectly
     */
    void setNavigationTargets(Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException;

    /**
     * Set error handler navigation targets.
     *
     * @param errorNavigationTargets
     *         error handler navigation targets
     */
    void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets);

    /**
     * Get the {@link RouteData} for all registered navigation targets.
     *
     * @return list of routes available for this registry
     */
    List<RouteData> getRegisteredRoutes();

    /**
     * Gets the optional navigation target class for a given path. Returns
     * an empty optional if no navigation target corresponds to the given
     * path.
     *
     * @param pathString
     *         the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given
     * path
     */
    Optional<Class<? extends Component>> getNavigationTarget(String pathString);

    /**
     * Gets the optional navigation target class for a given Location matching
     * with path segments.
     *
     * @param pathString
     *         path to get navigation target for, not {@code null}
     * @param segments
     *         segments given for path
     * @return optional navigation target corresponding to the given location
     * with given segments if any applicable targets found.
     * @see Location
     */
    Optional<Class<? extends Component>> getNavigationTarget(String pathString,
            List<String> segments);

    /**
     * Get a registered navigation target for given exception. First we will
     * search for a matching cause for in the exception chain and if no match
     * found search by extended type.
     *
     * @param exception
     *         exception to search error view for
     * @return optional error target entry corresponding to the given exception
     */
    Optional<ErrorTargetEntry> getErrorNavigationTarget(Exception exception);

    /**
     * Checks if the registry contains a route to the given path.
     *
     * @param pathString
     *         path to get navigation target for, not {@code null}
     * @return true if the registry contains a route to the given path, false
     * otherwise.
     */
    boolean hasRouteTo(String pathString);

    /**
     * Get the url string for given navigation target.
     *
     * @param navigationTarget
     *         navigation target to get registered route for, not
     *         {@code null}
     * @return optional navigation target url string
     */
    Optional<String> getTargetUrl(Class<? extends Component> navigationTarget);

    /**
     * Checks whether any navigation targets have been registered.
     *
     * @return <code>true</code> if at least one navigation target is
     * registered; otherwise <code>false</code>
     */
    boolean hasNavigationTargets();

    /**
     * Gets the {@link ThemeDefinition} associated with the given navigation
     * target, if any. The theme is defined by using the {@link Theme}
     * annotation on the navigation target class.
     * <p>
     * If no {@link Theme} and {@link NoTheme} annotation are used, by default
     * the {@code com.vaadin.flow.theme.lumo.Lumo} class is used (if present on
     * the classpath).
     *
     * @param navigationTarget
     *         the navigation target class
     * @param path
     *         the resolved route path so we can determine what the rendered
     *         target is for
     * @return the associated ThemeDefinition, or empty if none is defined and
     * the Lumo class is not in the classpath, or if the NoTheme
     * annotation is being used.
     */
    Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path);
}
