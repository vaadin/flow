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
import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * Contains all relevant information related to a valid navigation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class NavigationState implements Serializable {

    private Class<? extends Component> navigationTarget;
    private RouteTarget routeTarget;
    private UrlParameters urlParameters = UrlParameters.empty();
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
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget != null ? navigationTarget
                : routeTarget != null ? routeTarget.getTarget() : null;
    }

    /**
     * Sets the navigation target of this state.
     *
     * @param navigationTarget
     *            navigation target
     */
    public void setNavigationTarget(Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget,
                "navigationTarget cannot be null");
        this.navigationTarget = navigationTarget;
    }

    /**
     * Sets the route target of this state.
     *
     * @param routeTarget
     *            the route target to set
     */
    void setRouteTarget(RouteTarget routeTarget) {
        Objects.requireNonNull(routeTarget,
                "routeTarget cannot be null");
        this.routeTarget = routeTarget;
    }

    /**
     * Gets the route target for this navigation state.
     * 
     * @return the route target to navigate to.
     */
    public RouteTarget getRouteTarget() {
        if (routeTarget == null && navigationTarget != null) {
            routeTarget = router.getRegistry().getRouteTarget(navigationTarget,
                    urlParameters);

            if (routeTarget != null) {
                assert navigationTarget.equals(routeTarget.getTarget());
            }
        }
        return routeTarget;
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
                    .getTargetUrl(getNavigationTarget(), getParameters())
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
    void setParameters(UrlParameters urlParameters) {
        assert urlParameters != null;

        this.urlParameters = urlParameters;
    }

    /**
     * Gets the url parameters map.
     * 
     * @return url parameters.
     */
    public UrlParameters getParameters() {
        return urlParameters;
    }

    /**
     * Gets the list of strings that correspond to the raw string url
     * parameters.
     *
     * @return the url parameters of this navigation state
     */
    public Optional<List<String>> getUrlParameters() {
        return Optional.of(HasUrlParameterFormat
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
        setParameters(HasUrlParameterFormat.getParameters(urlParameters));
    }
}
