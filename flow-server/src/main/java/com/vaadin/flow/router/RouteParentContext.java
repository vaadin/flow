/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.Objects;

import com.vaadin.flow.component.Component;

/**
 * The context handed to a {@link RouteParentResolver} when resolving the
 * logical parent of a route without an instance.
 * <p>
 * It exposes the navigation target class whose parent is being resolved and the
 * {@link RouteParameters} it is resolved with, which is everything an
 * instance-free parent resolver needs to compute the parent reference (for
 * example to carry over a subset of the parameters to the parent route).
 *
 * @param navigationTarget
 *            the navigation target class whose parent is resolved, never
 *            {@code null}
 * @param routeParameters
 *            the route parameters the navigation target is resolved with, never
 *            {@code null}; {@link RouteParameters#empty()} when no parameters
 *            are available
 * @author Vaadin Ltd
 */
public record RouteParentContext(Class<? extends Component> navigationTarget,
        RouteParameters routeParameters) implements Serializable {

    /**
     * Creates a new context.
     *
     * @param navigationTarget
     *            the navigation target class, not {@code null}
     * @param routeParameters
     *            the route parameters, not {@code null}
     */
    public RouteParentContext {
        Objects.requireNonNull(navigationTarget,
                "navigationTarget must not be null");
        Objects.requireNonNull(routeParameters,
                "routeParameters must not be null");
    }

    /**
     * Gets the navigation target class whose parent is resolved.
     *
     * @return the navigation target class, never {@code null}
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Gets the route parameters the navigation target is resolved with.
     *
     * @return the route parameters, never {@code null}
     */
    public RouteParameters getRouteParameters() {
        return routeParameters;
    }
}
