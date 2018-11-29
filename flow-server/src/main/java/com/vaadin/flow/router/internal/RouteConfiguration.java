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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.startup.RouteTarget;

public class RouteConfiguration implements Serializable {

    private final boolean mutable;

    private final Map<String, RouteTarget> routes = new HashMap<>();
    private final Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();

    public RouteConfiguration() {
        mutable = false;
    }

    public RouteConfiguration(RouteConfiguration configuration,
            boolean mutable) {
        for (Map.Entry<String, RouteTarget> route : configuration.routes
                .entrySet()) {
            routes.put(route.getKey(), route.getValue().copy());
        }
        targetRoutes.putAll(configuration.targetRoutes);

        this.mutable = mutable;
    }

    public void clear() {
        throwIfImmutable();
        routes.clear();
        targetRoutes.clear();
    }

    private void throwIfImmutable() {
        if (!mutable) {
            throw new IllegalStateException("Configuration is immutable");
        }
    }

    /**
     * See if configuration contains a registered route for given path.
     *
     * @param path
     *         path to check
     * @return true if configuration contains route
     */
    public boolean hasRoute(String path) {
        return routes.containsKey(path);
    }

    public void addRouteTarget(String path, Class<? extends Component> routeTarget) {
        throwIfImmutable();
        routes.get(path).addRoute(routeTarget);
    }

    public void putTargetRoute(Class<? extends Component> navigationTarget,
            String path) {
        throwIfImmutable();
        targetRoutes.put(navigationTarget, path);
    }

    public void setRouteTarget(String path, RouteTarget routeTarget) {
        throwIfImmutable();
        routes.put(path, routeTarget);
    }

    /* Getters and other read methods. */
    public boolean hasRoute(String pathString, List<String> segments) {
        if(routes.containsKey(pathString)) {
            return routes.get(pathString).getTarget(segments) != null;
        }
        return false;
    }

    public Optional<Class<? extends Component>> getRoute(String pathString,
            List<String> segments) {
        if(routes.containsKey(pathString)) {
            RouteTarget routeTarget = routes.get(pathString);
            return Optional.ofNullable(routeTarget.getTarget(segments));
        }
        return Optional.empty();
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }
}
