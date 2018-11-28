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
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.ErrorTargetEntry;

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
     * Get the layout chain for given navigation target.
     * <p>
     * This chain may be pre-defined or generated from annotation data.
     *
     * @param navigationTarget
     *         navigation target to get layout chain for
     * @return layout chain of target
     */
    List<Class<? extends RouterLayout>> getRouteLayouts(
            Class<? extends Component> navigationTarget);

    /**
     * Get the layout chain for given navigation target on the targeted path.
     * <p>
     * This chain may be pre-defined or generated from annotation data.
     *
     * @param navigationTarget
     *         navigation target to get layout chain for
     * @param path
     *         path to use for resolving chain
     * @return layout chain of target
     */
    List<Class<? extends RouterLayout>> getRouteLayouts(
            Class<? extends Component> navigationTarget, String path);

    /**
     * Get the layout chain for an navigation target without a route annotation.
     *
     * @param nonRouteTarget
     *         target to get layouts for
     * @return layout chain for target
     */
    List<Class<? extends RouterLayout>> getNonRouteLayouts(
            Class<? extends Component> nonRouteTarget);
}
