/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Map;

import com.vaadin.flow.router.RouteParameters;

/**
 * Contains the information resulted from searching a route target using a
 * navigation url as input.
 *
 * The result of the search contains the target itself if found, and the url
 * parameter values extracted from the input path according with the route
 * configuration.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class NavigationRouteTarget implements Serializable {

    // Processed path.
    private final String path;

    // Target found for the specified path.
    private final RouteTarget routeTarget;

    // Parameters found in the path.
    private final RouteParameters parameters;

    public NavigationRouteTarget(String path, RouteTarget routeTarget,
            Map<String, String> parameters) {
        this.path = path;
        this.routeTarget = routeTarget;
        this.parameters = new RouteParameters(parameters);
    }

    /**
     * Gets whether this search result instance contains a navigation target.
     *
     * @return true if this search result instance contains a navigation target,
     *         otherwise false.
     */
    public boolean hasTarget() {
        return routeTarget != null;
    }

    /**
     * Gets the input path for the search.
     *
     * @return the input path for the search.
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the route target.
     *
     * @return the route target.
     */
    public RouteTarget getRouteTarget() {
        return routeTarget;
    }

    /**
     * Gets the route parameters for this search response.
     *
     * @return the route parameters for this search response.
     */
    public RouteParameters getRouteParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "[url: \"" + path + "\", target: "
                + (routeTarget != null ? routeTarget.getTarget() : null)
                + ", parameters: " + parameters + "]";
    }
}
