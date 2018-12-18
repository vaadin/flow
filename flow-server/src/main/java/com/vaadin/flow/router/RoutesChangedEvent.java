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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import com.vaadin.flow.server.RouteRegistry;

/**
 * Result class containing the removed and added routes for the latest
 * configuration.
 */
public class RoutesChangedEvent extends EventObject {

    private final List<RouteData> added = new ArrayList<>();
    private final List<RouteData> removed = new ArrayList<>();

    /**
     * Constructs a prototypical Event.
     *
     * @param source
     *         The object on which the Event initially occurred.
     * @throws IllegalArgumentException
     *         if source is null.
     */
    public RoutesChangedEvent(RouteRegistry source) {
        super(source);
    }

    @Override
    public RouteRegistry getSource() {
        return (RouteRegistry) super.getSource();
    }

    /**
     * Add a route that has been added to the event.
     *
     * @param addedRoute
     *         route that has been added
     */
    public void addRoute(RouteData addedRoute) {
        added.add(addedRoute);
    }

    /**
     * Add a route that has been removed to the event.
     *
     * @param removedRoute
     *         route that has been removed
     */
    public void removeRoute(RouteData removedRoute) {
        removed.add(removedRoute);
    }

    /**
     * Get all routes added for this change.
     *
     * @return list of all added routes
     */
    public List<RouteData> getAddedRoutes() {
        return added;
    }

    /**
     * Get all routes removed in this change.
     *
     * @return list of all removed routes
     */
    public List<RouteData> getRemovedRoutes() {
        return removed;
    }

}
