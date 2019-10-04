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

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

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

}
