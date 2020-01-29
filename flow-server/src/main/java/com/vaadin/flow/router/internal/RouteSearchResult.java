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
import java.util.Collections;
import java.util.Map;

class RouteSearchResult {

    // Processed path.
    private String path;

    // Target found for the specified path.
    private RouteTarget target;

    // Parameters found in the path.
    private Map<String, Serializable> urlParameters;

    RouteSearchResult(String path, RouteTarget target,
            Map<String, Serializable> urlParameters) {
        this.path = path;
        this.target = target;
        this.urlParameters = Collections.unmodifiableMap(urlParameters);
    }

    public boolean hasTarget() {
        return target != null;
    }

    public String getPath() {
        return path;
    }

    public RouteTarget getTarget() {
        return target;
    }

    /**
     * Gets the url parameters for this search response.
     * 
     * @return the url parameters for this search response.
     */
    public Map<String, Serializable> getUrlParameters() {
        return urlParameters;
    }

    @Override
    public String toString() {
        return "[path: \"" + path + "\", target: "
                + (target != null ? target.getRoutes().toString() : null)
                + ", parameters: " + urlParameters + "]";
    }
}
