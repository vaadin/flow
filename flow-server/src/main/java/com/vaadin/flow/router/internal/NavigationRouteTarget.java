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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Map;

import com.vaadin.flow.router.UrlParameters;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * Contains the information resulted from searching a route target using a
 * navigation url as input.
 * 
 * The result of the search contains the target itself if found, and the url
 * parameter values extracted from the input path according with the route
 * configuration.
 */
public class NavigationRouteTarget implements Serializable {

    // Processed path.
    private String url;

    // Target found for the specified path.
    private RouteTarget target;

    // Parameters found in the path.
    private UrlParameters urlParameters;

    NavigationRouteTarget(String url, RouteTarget target,
            Map<String, String> urlParameters) {
        this.url = url;
        this.target = target;
        this.urlParameters = new UrlParameters(urlParameters);
    }

    /**
     * Gets whether this search result instance contains a navigation target.
     * 
     * @return true if this search result instance contains a navigation target,
     *         otherwise false.
     */
    public boolean hasTarget() {
        return target != null;
    }

    /**
     * Gets the input path for the search.
     * 
     * @return the input path for the search.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the navigation target.
     * 
     * @return the navigation target.
     */
    public RouteTarget getTarget() {
        return target;
    }

    /**
     * Gets the url parameters for this search response.
     * 
     * @return the url parameters for this search response.
     */
    public UrlParameters getUrlParameters() {
        return urlParameters;
    }

    @Override
    public String toString() {
        return "[url: \"" + url + "\", target: "
                + (target != null ? target.getTarget() : null)
                + ", parameters: " + urlParameters + "]";
    }
}
