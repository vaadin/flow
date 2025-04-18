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
package com.vaadin.flow.router;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.RouteTarget;

/**
 * A builder class for constructing new {@link NavigationState} instances.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class NavigationStateBuilder {

    private NavigationState currentState;

    private final Router router;

    /**
     * Constructs a new NavigationStateBuilder.
     *
     * @param router
     *            the router managing navigation
     */
    public NavigationStateBuilder(Router router) {
        currentState = new NavigationState(router);
        this.router = router;
    }

    /**
     * Assigns the given navigation target with the given url parameter to the
     * navigation state being built.
     *
     * @param navigationTarget
     *            the navigation target
     * @param parameters
     *            the route parameters of the navigation target
     * @return this builder, for chaining
     */
    public NavigationStateBuilder withTarget(
            Class<? extends Component> navigationTarget,
            RouteParameters parameters) {
        currentState.setNavigationTarget(navigationTarget);
        currentState.setRouteParameters(parameters);
        return this;
    }

    /**
     * Assigns the given route target with the given route parameters to the
     * navigation state being built.
     *
     * @param routeTarget
     *            the route target
     * @param parameters
     *            the route parameters of the navigation target
     * @return this builder, for chaining
     */
    public NavigationStateBuilder withTarget(RouteTarget routeTarget,
            RouteParameters parameters) {
        currentState.setRouteTarget(routeTarget);
        currentState.setRouteParameters(parameters);
        return this;
    }

    /**
     * Assigns the given navigation target to the navigation state being built.
     *
     * @param navigationTarget
     *            the navigation target
     * @return this builder, for chaining
     */
    public NavigationStateBuilder withTarget(
            Class<? extends Component> navigationTarget) {
        currentState.setNavigationTarget(navigationTarget);
        return this;
    }

    /**
     * Assign the path that was used for determining the navigation target.
     *
     * @param path
     *            navigation path
     * @return this builder, for chaining
     */
    public NavigationStateBuilder withPath(String path) {
        currentState.setResolvedPath(path);
        return this;
    }

    /**
     * Returns the NavigationState instance that has been built so far and
     * resets the internal state of this builder.
     *
     * @return the built NavigationState instance
     */
    public NavigationState build() {
        NavigationState toReturn = currentState;
        currentState = new NavigationState(router);
        return toReturn;
    }
}
