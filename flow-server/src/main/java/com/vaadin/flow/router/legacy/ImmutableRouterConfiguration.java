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
package com.vaadin.flow.router.legacy;

import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationHandler;

/**
 * Configuration for a {@link Router}. Use
 * {@link Router#reconfigure(RouterConfigurator)} to update the configuration
 * used by a {@link Router}.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public interface ImmutableRouterConfiguration {
    /**
     * Gets the resolver to use for resolving what to show for a given
     * navigation event.
     *
     * @return the resolver, not <code>null</code>
     */
    Resolver getResolver();

    /**
     * Checks whether this configuration can be modified.
     *
     * @return <code>true</code> if it is modifiable, <code>false</code> if it
     *         immutable
     */
    boolean isModifiable();

    /**
     * Resolves a route.
     *
     * @param location
     *            the location to resolve, not <code>null</code>
     * @return an optional navigation handler for handling the route, or empty
     *         optional if no configured route matched the location
     */
    Optional<NavigationHandler> resolveRoute(Location location);

    /**
     * Gets the parent type configured for the given view type.
     *
     * @param viewType
     *            the view type for which to find a parent, not
     *            <code>null</code>
     * @return the parent view type, or an empty optional if no parent view has
     *         been set
     */
    Optional<Class<? extends HasChildView>> getParentView(
            Class<? extends View> viewType);

    /**
     * Gets the parent types configured for the given view type, starting from the most close to the view and
     * ending with the topmost in the hierarchy.
     * <p>
     * The returned stream is ordered, includes the parent view as returned by
     * {@link #getParentView(Class)} and recursively up until a view which does
     * not have a parent view.
     *
     * @param viewType
     *            the view type for which to find the parent views, not
     *            <code>null</code>
     * @return a stream of parent view types
     */
    Stream<Class<? extends HasChildView>> getParentViewsAscending(
            Class<? extends View> viewType);

    /**
     * Gets the configured routes for the given view type.
     * <p>
     * Only returns the routes for explicitly mapped views. Any routes mapped to
     * a custom {@link NavigationHandler} will not be returned.
     *
     * @param viewType
     *            the view type for which to find routes, not <code>null</code>
     * @return the routes configured for the given view type
     */
    Stream<String> getRoutes(Class<? extends View> viewType);

    /**
     * Gets the configured route for the given view type.
     * <p>
     * Only returns the route for explicitly mapped views. Any route mapped to a
     * custom {@link NavigationHandler} will not be returned.
     * <p>
     * Throws an exception if there are multiple routes configured for the given
     * view type.
     *
     * @param viewType
     *            the view type for which to find a route, not <code>null</code>
     * @return the configured route for the given view type
     * @throws IllegalArgumentException
     *             if multiple routes have been defined for the given view type
     */
    Optional<String> getRoute(Class<? extends View> viewType)
            throws IllegalArgumentException;

    /**
     * Gets the {@link PageTitleGenerator} to use for creating a new
     * {@link Page#setTitle(String) page title} according to the navigation.
     * <p>
     * The default is the {@link DefaultPageTitleGenerator}.
     *
     * @return the page title generator, never <code>null</code>
     */
    PageTitleGenerator getPageTitleGenerator();

    /**
     * Checks whether this configuration has been configured. The configuration
     * of a {@link Router} is considered configured if
     * {@link RouterConfigurator} has been used or if
     * {@link Router#reconfigure(RouterConfigurator)} has been run at least
     * once.
     *
     * @return <code>true</code> if this instance has been configured,
     *         <code>false</code> otherwise
     */
    boolean isConfigured();

    /**
     * Gets the error handler.
     * <p>
     * The error handler shows views corresponding to the 404 error page. It is
     * used when the user tries to navigate into an undefined route.
     * <p>
     * By default returns a {@link StaticViewRenderer} configured to show
     * {@link DefaultErrorView}.
     *
     * @return the error view handler, not <code>null</code>
     */
    NavigationHandler getErrorHandler();
}
