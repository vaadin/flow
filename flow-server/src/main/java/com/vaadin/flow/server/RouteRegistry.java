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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.shared.Registration;

/**
 * The RouteRegistry interface class that gives the out facing usage needs for a
 * route registry implementation.
 * <p>
 * This is used by {@link Router} when resolving navigation and layout chain
 * population.
 *
 * @since 1.3
 */
public interface RouteRegistry extends Serializable {

    /**
     * Register a navigation target with specified path and given parent layout
     * chain. Any {@link ParentLayout}, {@link Route} or {@link RouteAlias} will
     * be ignored in route handling.
     *
     * @param path
     *            path to register navigation target to
     * @param navigationTarget
     *            navigation target to register into session scope
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    void setRoute(String path, Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain);

    /**
     * Remove the given navigation target route registration. Path where the
     * navigation target was may still be usable, e.g. we remove target with url
     * param and there is left a non param target, but will not return the
     * removed target.
     * <p>
     * Note! this will remove target route and if possible any
     * {@link RouteAlias} route that can be found for the class.
     *
     * @param navigationTarget
     *            navigation target class to remove
     */
    void removeRoute(Class<? extends Component> navigationTarget);

    /**
     * Remove all registrations for given path. This means that any navigation
     * target registered on the given path will be removed. But if a removed
     * navigationTarget for the path exists it is then stored with a new main
     * path so it can still get a resolved url.
     * <p>
     * E.g. path "home" contains HomeView and DetailsView[String path param]
     * both will be removed.
     * <p>
     * Note! The restored path will be the first found match for all paths that
     * are registered.
     *
     * @param path
     *            path for which to remove all navigation targets
     */
    void removeRoute(String path);

    /**
     * Remove only the specified navigationTarget from the path and not the
     * whole path if other targets exist for path. If no other targets exist
     * whole route will be cleared.
     * <p>
     * This will leave any other targets for path e.g. removing the wildcard
     * path will still leave the optional target.
     * <p>
     * Note! If another path exists for the removed navigation target it will
     * get a new main path so it can still get a resolved url. The restored path
     * will be the first found match for all paths that are registered.
     *
     * @param path
     *            path to remove from registry
     * @param navigationTarget
     *            path navigation target to remove
     */
    void removeRoute(String path, Class<? extends Component> navigationTarget);

    /**
     * Get the {@link RouteData} for all registered navigation targets.
     *
     * @return list of routes available for this registry
     */
    List<RouteData> getRegisteredRoutes();

    /**
     * Gets the optional navigation target class for a given path. Returns an
     * empty optional if no navigation target corresponds to the given path.
     * <p>
     * Note! If a path has been specifically removed from a registry it will not
     * be served from possible higher lever registries either.
     *
     * @param pathString
     *            the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given path
     */
    Optional<Class<? extends Component>> getNavigationTarget(String pathString);

    /**
     * Gets the optional navigation target class for a given Location matching
     * with path segments.
     * <p>
     * Note! If a path has been specifically removed from a registry it will not
     * be served from possible higher lever registries either.
     *
     * @param pathString
     *            path to get navigation target for, not {@code null}
     * @param segments
     *            segments given for path
     * @return optional navigation target corresponding to the given location
     *         with given segments if any applicable targets found.
     * @see Location
     */
    Optional<Class<? extends Component>> getNavigationTarget(String pathString,
            List<String> segments);

    /**
     * Get the url string for given navigation target.
     * <p>
     * Will return Optional.empty is navigation target was not found.
     *
     * @param navigationTarget
     *            navigation target to get registered route for, not
     *            {@code null}
     * @return optional navigation target url string
     */
    Optional<String> getTargetUrl(Class<? extends Component> navigationTarget);

    /**
     * Get the layout chain for given navigation target on the targeted path.
     * <p>
     * This chain may be pre-defined or generated from annotation data.
     *
     * @param path
     *            path to use for resolving chain
     * @param navigationTarget
     *            navigation target to get layout chain for
     * @return layout chain of target
     */
    List<Class<? extends RouterLayout>> getRouteLayouts(String path,
            Class<? extends Component> navigationTarget);

    /**
     * Block updates to the registry configuration from other threads until
     * update command has completed. This makes the command changes atomic for
     * the registry as no one else can change the registry state during the
     * command.
     * <p>
     * Any other thread trying to configure current registry will be blocked
     * until the update has released all the locks.
     *
     * @param command
     *            command to execute for the update
     */
    void update(Command command);

    /**
     * Adds the given route change listener to the registry.
     *
     * @param listener
     *            listener to be informed on route changes
     * @return registration to remove the listener
     */
    Registration addRoutesChangeListener(RoutesChangedListener listener);

    /**
     * Clear all registered routes from the registry.
     */
    void clean();
}
