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
 * A reference to a route in a logical navigation hierarchy, consisting of the
 * navigation target class and the {@link RouteParameters} it should be resolved
 * with.
 * <p>
 * It is returned by a {@link RouteParentResolver} to describe a logical parent
 * and is the element type of a resolved breadcrumb trail. The parameters are
 * part of the reference because a parent route typically inherits only a subset
 * of the child parameters (for example {@code orgs/:orgId} as the parent of
 * {@code orgs/:orgId/projects/:projectId}), and that subset is needed both to
 * resolve the parent's own title and to resolve its parent in turn.
 *
 * @param navigationTarget
 *            the navigation target class, never {@code null}
 * @param routeParameters
 *            the route parameters to resolve the navigation target with, never
 *            {@code null}
 * @author Vaadin Ltd
 */
public record RouteParentReference(Class<? extends Component> navigationTarget,
        RouteParameters routeParameters) implements Serializable {

    /**
     * Creates a new reference.
     *
     * @param navigationTarget
     *            the navigation target class, not {@code null}
     * @param routeParameters
     *            the route parameters, not {@code null}
     */
    public RouteParentReference {
        Objects.requireNonNull(navigationTarget,
                "navigationTarget must not be null");
        Objects.requireNonNull(routeParameters,
                "routeParameters must not be null");
    }

    /**
     * Creates a new reference with empty route parameters.
     *
     * @param navigationTarget
     *            the navigation target class, not {@code null}
     */
    public RouteParentReference(Class<? extends Component> navigationTarget) {
        this(navigationTarget, RouteParameters.empty());
    }

    /**
     * Gets the navigation target class.
     *
     * @return the navigation target class, never {@code null}
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Gets the route parameters to resolve the navigation target with.
     *
     * @return the route parameters, never {@code null}
     */
    public RouteParameters getRouteParameters() {
        return routeParameters;
    }
}
