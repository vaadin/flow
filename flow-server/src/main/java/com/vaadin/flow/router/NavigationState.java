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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.RouteTarget;

/**
 * Contains all relevant information related to a valid navigation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class NavigationState implements Serializable {

    private RouteTarget navigationTarget;
    private UrlParameters urlParameters;
    private String resolvedPath;
    private final Router router;

    /**
     * Creates a new instance of the class using the {@code router}.
     *
     * @param router
     *            the router managing navigation
     */
    public NavigationState(Router router) {
        this.router = router;
    }

    /**
     * Gets the navigation target of this state.
     *
     * @return the navigation target of this state
     */
//    public Class<? extends Component> getNavigationTarget() {
//        return navigationTarget.getTarget();
//    }

    public RouteTarget getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Sets the navigation target of this state.
     *
     * @param navigationTarget
     *            navigation target
     * @deprecated use {@link #setNavigationTarget(RouteTarget)} instead.
     */
    public void setNavigationTarget(
            Class<? extends Component> navigationTarget) {
        this.navigationTarget = router.getRegistry()
                .getRouteTarget(navigationTarget, urlParameters);
    }

    /**
     * Sets the navigation target of this state.
     *
     * @param navigationTarget
     *            the navigation target to set
     */
    public void setNavigationTarget(
            RouteTarget navigationTarget) {
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
            resolvedPath = router.getRegistry()
                    .getTargetUrl(navigationTarget.getTarget(), urlParameters)
                    .orElse(null);
        }
        return resolvedPath;
    }

    /**
     * Sets the url parameters.
     * 
     * @param urlParameters
     *            url parameters.
     */
    public void setParameters(UrlParameters urlParameters) {
        this.urlParameters = urlParameters;
    }

    /**
     * Gets the url parameters map.
     * 
     * @return url parameters.
     */
    public UrlParameters getParameters() {
        return urlParameters != null ? urlParameters : new UrlParameters(null);
    }

    /**
     * Gets the list of strings that correspond to the raw string url
     * parameters.
     *
     * @return the url parameters of this navigation state
     * @deprecated use {@link #getParameters()}
     */
    @Deprecated
    public Optional<List<String>> getUrlParameters() {
        return Optional.of(HasUrlParameterUtil
                .getParameterValues(getParameters()));
    }

    /**
     * Set the list of strings that correspond to the raw string url parameters.
     *
     * @param urlParameters
     *            the url parameters to set
     * @deprecated use {@link #setParameters(UrlParameters)} instead.
     */
    @Deprecated
    public void setUrlParameters(List<String> urlParameters) {
        setParameters(HasUrlParameterUtil.getParameters(urlParameters));
    }
}
