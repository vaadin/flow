/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.ui.Page;

/**
 * Configuration for a {@link Router}. Use
 * {@link Router#reconfigure(RouterConfigurator)} to update the configuration
 * used by a {@link Router}.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface RouterConfiguration {
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
     * @return a navigation handler or handling the route, or <code>null</code>
     *         if no configured route matched the location
     */
    NavigationHandler resolveRoute(Location location);

    /**
     * Gets the parent type configured for the given view type.
     *
     * @param viewType
     *            the view type for which to find a parent, not
     *            <code>null</code>
     * @return the parent view type, or <code>null</code> if no parent view has
     *         been set
     */
    Class<? extends HasChildView> getParentView(Class<? extends View> viewType);

    /**
     * Gets the parent types configured for the given view type.
     * <p>
     * The returned list includes the parent view as returned by
     * {@link #getParentView(Class)} and recursively up until a view which does
     * not have a parent view.
     *
     * @param viewType
     *            the view type for which to find the parent views, not
     *            <code>null</code>
     * @return a stream of parent view types
     */
    Stream<Class<? extends HasChildView>> getParentViews(
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
}
