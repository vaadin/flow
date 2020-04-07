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
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.RouteTarget;
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
     * Register a navigation target with specified template and given parent
     * layout chain. Any {@link ParentLayout}, {@link Route} or
     * {@link RouteAlias} will be ignored in route handling.
     *
     * @param template
     *            template to register navigation target to
     * @param navigationTarget
     *            navigation target to register into session scope
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    void setRoute(String template, Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain);

    /**
     * Remove the given navigation target route registration.
     * <p>
     * Note! this will remove target route and if possible any
     * {@link RouteAlias} route that can be found for the class.
     *
     * @param navigationTarget
     *            navigation target class to remove
     */
    void removeRoute(Class<? extends Component> navigationTarget);

    /**
     * Remove the registration for given path.
     * <p>
     * In case navigationTarget is a
     * {@link com.vaadin.flow.router.HasUrlParameter}, path argument needs to
     * include the parameter placeholder which is added automatically.
     * Otherwise, using {@link #removeRoute(String, Class)} is preferred in such
     * a case.
     *
     * @param path
     *            path for which to remove the navigation target.
     */
    void removeRoute(String path);

    /**
     * Remove navigationTarget for the path.
     * <p>
     * This method will check if indeed navigationTarget is registered with
     * path.
     * <p>
     * In case navigationTarget is a
     * {@link com.vaadin.flow.router.HasUrlParameter}, the path needs to be
     * specified exactly as it is in the {@link Route} annotation or as it was
     * registered using {@link #setRoute(String, Class, List)}, without the
     * parameter placeholder which is automatically added.
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
     * Search for a route target using given navigation <code>url</code>
     * argument.
     *
     * @param url
     *            the navigation url used to search a route target.
     * @return a {@link NavigationRouteTarget} instance containing the
     *         {@link RouteTarget} and {@link RouteParameters} extracted from the
     *         <code>url</code> argument according with the route configuration.
     */
    NavigationRouteTarget getNavigationRouteTarget(String url);

    /**
     * Gets the {@link RouteTarget} instance matching the given target component
     * and route parameters.
     * 
     * @param target
     *            a component class which is a navigation target.
     * @param parameters
     *            parameter values that may be used with given target.
     * @return the {@link RouteTarget} instance matching the given target
     *         component and route parameters.
     */
    RouteTarget getRouteTarget(Class<? extends Component> target,
                               RouteParameters parameters);

    /**
     * Gets the optional navigation target class for a given path. Returns an
     * empty optional if no navigation target corresponds to the given url.
     *
     * @param url
     *            the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given path
     */
    Optional<Class<? extends Component>> getNavigationTarget(String url);

    /**
     * Gets the optional navigation target class for a given Location matching
     * with path segments.
     *
     * @param url
     *            path to get navigation target for, not {@code null}
     * @param segments
     *            segments given for path
     * @return optional navigation target corresponding to the given location
     *         with given segments if any applicable targets found.
     * @see Location
     */
    Optional<Class<? extends Component>> getNavigationTarget(String url,
            List<String> segments);

    /**
     * Get the url string for given navigation target.
     *
     * @param navigationTarget
     *            navigation target to get registered route for, not
     *            {@code null}
     * @return {@link Optional} navigation target url string or
     *         {@link Optional#empty()} if navigation target was not found
     */
    Optional<String> getTargetUrl(Class<? extends Component> navigationTarget);

    /**
     * Get the url string for given navigation target.
     *
     * @param navigationTarget
     *            navigation target to get registered route for, not
     *            {@code null}
     * @param parameters
     *            parameters for the target url.
     * @return {@link Optional} navigation target url string or
     *         {@link Optional#empty()} if navigation target was not found
     */
    Optional<String> getTargetUrl(Class<? extends Component> navigationTarget,
            RouteParameters parameters);

    /**
     * Get the main template for given navigation target.
     * <p>
     * In case of annotated target the main template is composed of the
     * {@link Route} annotation value prefixed by all {@link RoutePrefix} values
     * of the parent {@link RouterLayout}s chain.
     *
     * @param navigationTarget
     *            navigation target to get route definition for, not
     *            {@code null}
     * @return {@link Optional} navigation target template string or
     *         {@link Optional#empty()} if navigation target was not found
     */
    Optional<String> getTemplate(
            Class<? extends Component> navigationTarget);

    /**
     * Get the layout chain for given navigation target on the targeted url.
     * <p>
     * This chain may be pre-defined or generated from annotation data.
     *
     * @param url
     *            url to use for resolving chain
     * @param navigationTarget
     *            navigation target to get layout chain for
     * @return layout chain of target
     * @deprecated use {@link #getNavigationRouteTarget(String)} instead.
     */
    @Deprecated
    List<Class<? extends RouterLayout>> getRouteLayouts(String url,
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
