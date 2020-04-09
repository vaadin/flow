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
import com.vaadin.flow.router.internal.RouteTarget;

/**
 * Contains all relevant information related to a valid navigation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class NavigationState implements Serializable {

    private Class<? extends Component> navigationTarget;
    private RouteTarget routeTarget;
    private RouteParameters routeParameters = RouteParameters.empty();
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
                    routeParameters);

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
                    .getTargetUrl(getNavigationTarget(), getRouteParameters())
                    .orElse(null);
        }
        return resolvedPath;
    }

    /**
     * Sets the route parameters.
     *
     * @param routeParameters
     *            route parameters.
     */
    void setRouteParameters(RouteParameters routeParameters) {
        assert routeParameters != null;

        this.routeParameters = routeParameters;
    }

    /**
     * Gets the route parameters map.
     * 
     * @return route parameters.
     */
    public RouteParameters getRouteParameters() {
        return routeParameters;
    }

    /**
     * Gets the list of strings that correspond to the raw string url
     * parameters.
     *
     * @return the url parameters of this navigation state
     */
    public Optional<List<String>> getUrlParameters() {
        return Optional.of(HasUrlParameterFormat
                .getParameterValues(getRouteParameters()));
    }

    /**
     * Set the list of strings that correspond to the raw string route parameters.
     *
     * @param parameters
     *            the url parameters to set
     * @deprecated use {@link #setRouteParameters(RouteParameters)} instead.
     */
    @Deprecated
    public void setUrlParameters(List<String> parameters) {
        setRouteParameters(HasUrlParameterFormat.getParameters(parameters));
    }
}
