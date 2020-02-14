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
package com.vaadin.flow.router;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.RouteRegistry;

/**
 * Result class containing the removed and added routes for the latest
 * configuration.
 *
 * @since 1.3
 */
public class RoutesChangedEvent extends EventObject {

    private final List<RouteBaseData<?>> added;
    private final List<RouteBaseData<?>> removed;

    /**
     * Constructs a prototypical Event.
     *
     * @param source
     *         The object on which the Event initially occurred.
     * @param added
     *         list of all the added routes
     * @param removed
     *         list of all the removed routes
     * @throws IllegalArgumentException
     *         if source is null.
     */
    public RoutesChangedEvent(RouteRegistry source,
            List<RouteBaseData<?>> added, List<RouteBaseData<?>> removed) {
        super(source);
        this.added = Collections.unmodifiableList(added);
        this.removed = Collections.unmodifiableList(removed);
    }

    @Override
    public RouteRegistry getSource() {
        return (RouteRegistry) super.getSource();
    }

    /**
     * Get all routes added for this change.
     *
     * @return immutable list of all added routes
     */
    public List<RouteBaseData<?>> getAddedRoutes() {
        return added;
    }

    /**
     * Get all routes removed in this change.
     *
     * @return immutable list of all removed routes
     */
    public List<RouteBaseData<?>> getRemovedRoutes() {
        return removed;
    }

    /**
     * Determines if a given route navigation target was added for this change.
     *
     * @param clazz a route navigation target
     * @return true if the route was added for this change and false otherwise
     */
    public boolean isRouteAdded(Class<? extends Component> clazz) {
        return checkIfRouteIsPresent(added, clazz);
    }

    /**
     * Determines if a given route navigation target was removed for this change.
     *
     * @param clazz a route navigation target
     * @return true if the route was removed for this change and false otherwise
     */
    public boolean isRouteRemoved(Class<? extends Component> clazz) {
        return checkIfRouteIsPresent(removed, clazz);
    }

    private boolean checkIfRouteIsPresent(List<RouteBaseData<?>> routes,
                                          Class<? extends Component> clazz) {
        return routes.stream()
                .map(RouteBaseData::getNavigationTarget)
                .anyMatch(navigationTarget -> navigationTarget.equals(clazz));
    }

    /**
     * Determines if a route url was added for this change.
     *
     * @param path The URL of a route
     * @return true if the route was added for this change and false otherwise
     */
    public boolean isPathAdded(String path) {
        return checkIfRouteIsPresent(added, path);
    }

    /**
     * Determines if a route url was removed for this change.
     *
     * @param path The URL of a route
     * @return true if the route was removed for this change and false otherwise
     */
    public boolean isPathRemoved(String path) {
        return checkIfRouteIsPresent(removed, path);
    }

    /**
     * Get every single navigation targets of all added routes in this change.
     *
     * @return immutable list of all added navigation targets
     */
    public List<Class<? extends Component>> getAddedNavigationTargets() {
        return Collections.unmodifiableList(added.stream()
                .map(RouteBaseData::getNavigationTarget)
                .collect(Collectors.toList()));
    }

    /**
     * Get every single navigation targets of all removed routes in this change.
     *
     * @return immutable list of all removed navigation targets
     */
    public List<Class<? extends Component>> getRemovedNavigationTargets() {
        return Collections.unmodifiableList(removed.stream()
                .map(RouteBaseData::getNavigationTarget)
                .collect(Collectors.toList()));
    }

    /**
     * Get every single URL of all added routes in this change.
     *
     * @return immutable list of all added URLs
     */
    public List<String> getAddedURLs() {
        return Collections.unmodifiableList(added.stream()
                .map(RouteBaseData::getUrl).collect(Collectors.toList()));
    }

    /**
     * Get every single URL of all removed routes in this change.
     *
     * @return immutable list of all removed URLs
     */
    public List<String> getRemovedURLs() {
        return Collections.unmodifiableList(removed.stream()
                .map(RouteBaseData::getUrl).collect(Collectors.toList()));
    }

    private boolean checkIfRouteIsPresent(List<RouteBaseData<?>> routes,
                                          String path) {
        return routes.stream()
                .map(RouteBaseData::getUrl)
                .anyMatch(url -> url.equals(path));
    }

}
