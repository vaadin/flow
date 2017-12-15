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
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.router.Route;
import com.vaadin.ui.Component;

/**
 * Class containing all relevant information related to a valid navigation.
 *
 * @author Vaadin Ltd.
 */
public class NavigationState implements Serializable {

    private Class<? extends Component> navigationTarget;
    private List<String> urlParameters;
    private String resolvedPath;

    /**
     * Gets the navigation target of this state.
     *
     * @return the navigation target of this state
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Sets the navigation target of this state.
     *
     * @param navigationTarget
     *            the navigation target to set
     */
    public void setNavigationTarget(
            Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget,
                "navigationTarget cannot be null");
        this.navigationTarget = navigationTarget;
    }

    /**
     * Set the path for the resolved navigation target.
     * 
     * @param resolvedPath
     *            path for which the target was selected
     */
    public void setResolvedPath(String resolvedPath) {
        this.resolvedPath = resolvedPath;
    }

    /**
     * Get the resolved path details for this navigation state.
     * 
     * @return the resolved path details
     */
    public String getResolvedPath() {
        if (resolvedPath == null) {
            resolvedPath = navigationTarget.getAnnotation(Route.class).value();
        }
        return resolvedPath;
    }

    /**
     * Gets the list of strings that correspond to the raw string url
     * parameters.
     *
     * @return the url parameters of this navigation state
     */
    public Optional<List<String>> getUrlParameters() {
        return Optional.ofNullable(urlParameters);
    }

    /**
     * Set the list of strings that correspond to the raw string url parameters.
     *
     * @param urlParameters
     *            the url parameters to set
     */
    public void setUrlParameters(List<String> urlParameters) {
        this.urlParameters = urlParameters;
    }
}
