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
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteParameterFormat;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.router.UrlParameters;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.server.startup.RouteTarget;
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
     * Register a navigation target with specified path template and given
     * parent layout chain. Any {@link ParentLayout}, {@link Route} or
     * {@link RouteAlias} will be ignored in route handling.
     *
     * @param urlTemplate
     *            path template to register navigation target to
     * @param navigationTarget
     *            navigation target to register into session scope
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    void setRoute(String urlTemplate, Class<? extends Component> navigationTarget,
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
     * Remove the registration for given url template.
     *
     * @param urlTemplate
     *            urlTemplate for which to remove the navigation target.
     */
    void removeRoute(String urlTemplate);

    /**
     * Remove navigationTarget from the urlTemplate.
     *
     * @param urlTemplate
     *            urlTemplate to remove from registry
     * @param navigationTarget
     *            urlTemplate navigation target to remove
     * @deprecated use {@link #removeRoute(String)} or
     *             {@link #removeRoute(Class)} instead.
     */
    @Deprecated
    void removeRoute(String urlTemplate, Class<? extends Component> navigationTarget);

    /**
     * Get the {@link RouteData} for all registered navigation targets.
     *
     * @return list of routes available for this registry
     */
    List<RouteData> getRegisteredRoutes();

    /**
     * Search for a route navigation target and extract the necessary url
     * parameters from the <code>url</code> argument.
     *
     * @param url
     *            the navigation url to search for navigation targets with.
     * @return a {@link NavigationRouteTarget} instance containing the navigation
     *         target and parameter values extracted from the <code>url</code>
     *         argument according with the route configuration.
     */
    NavigationRouteTarget getNavigationRouteTarget(String url);

    /**
     * Gets the {@link RouteTarget} instance matching the given target component
     * and url parameters.
     * 
     * @param target
     *            a component class which is a navigation target.
     * @param parameters
     *            parameter values that may be used with given target.
     * @return the {@link RouteTarget} instance matching the given target
     *         component and url parameters.
     */
    RouteTarget getRouteTarget(Class<? extends Component> target,
            UrlParameters parameters);

    /**
     * Gets the optional navigation target class for a given path. Returns an
     * empty optional if no navigation target corresponds to the given url.
     *
     * @param pathString
     *            the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given path
     */
    Optional<Class<? extends Component>> getNavigationTarget(String pathString);

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
     * Get the url string for given navigation target.
     * <p>
     * Will return Optional.empty is navigation target was not found.
     *
     * @param navigationTarget
     *            navigation target to get registered route for, not
     *            {@code null}
     * @param parameters
     *            parameters for the target url.
     * @return optional navigation target url string
     */
    Optional<String> getTargetUrl(Class<? extends Component> navigationTarget,
            UrlParameters parameters);

    /**
     * Get a route representation for given navigation target.
     * <p>
     * Will return Optional.empty is navigation target was not found.
     *
     * @param navigationTarget
     *            navigation target to get route definition for, not
     *            {@code null}
     * @return optional navigation target url string
     */
    Optional<String> getUrlTemplate(
            Class<? extends Component> navigationTarget);

    /**
     * Get a url template for given navigation target with parameters formatted
     * according to the given format.
     * <p>
     * Will return Optional.empty is navigation target was not found.
     *
     * @param navigationTarget
     *            navigation target to get route definition for, not
     *            {@code null}
     * @param format
     *            settings used to format the result parameters.
     * @return optional navigation target url string
     */
    Optional<String> getUrlTemplate(Class<? extends Component> navigationTarget,
            Set<RouteParameterFormat> format);

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
