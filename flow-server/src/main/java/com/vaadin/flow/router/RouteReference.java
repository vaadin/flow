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
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;

/**
 * A reference to a route, consisting of a navigation target class and the
 * {@link RouteParameters} it should be resolved with. It identifies a route
 * regardless of whether the route has a logical parent, and can resolve its own
 * {@link #getPageTitle() page title} without instantiating the navigation
 * target.
 * <p>
 * It is returned by a {@link RouteParentResolver} to describe a logical parent
 * and is the element type of a resolved breadcrumb trail. The parameters are
 * part of the reference because a parent route typically inherits only a subset
 * of the child parameters (for example {@code orgs/:orgId} as the parent of
 * {@code orgs/:orgId/projects/:projectId}), and that subset is needed both to
 * resolve the parent's own title and to resolve its parent in turn.
 *
 * @param navigationTarget
 *            the navigation target class
 * @param routeParameters
 *            the route parameters to resolve the navigation target with
 * @author Vaadin Ltd
 */
public record RouteReference(Class<? extends Component> navigationTarget,
        RouteParameters routeParameters) implements Serializable {

    /**
     * Resolves the page title of this route without creating an instance of its
     * navigation target, using empty query parameters.
     * <p>
     * This is a convenience for resolving the titles of the entries returned by
     * {@link RouteConfiguration#getRouteHierarchy(Class, RouteParameters)}, for
     * example to build a breadcrumb trail.
     *
     * @return the resolved title, or an empty {@link Optional} if the target
     *         declares no title and no default generator is available
     * @see #getPageTitle(QueryParameters)
     */
    public Optional<String> getPageTitle() {
        return getPageTitle(QueryParameters.empty());
    }

    /**
     * Resolves the page title of this route without creating an instance of its
     * navigation target.
     * <p>
     * The title is resolved in this order:
     * <ol>
     * <li>the per-route {@link DynamicPageTitle} generator;</li>
     * <li>the application-wide default {@link PageTitleGenerator};</li>
     * <li>the static {@link PageTitle#value()}.</li>
     * </ol>
     * The generators and the default generator are obtained from the current
     * {@link VaadinService}; when no service is active, only annotation-based
     * resolution is performed.
     * <p>
     * Since the navigation target is not instantiated,
     * {@link HasDynamicTitle#getPageTitle()} is not consulted; the title of an
     * instantiated, currently shown route may therefore differ from the result
     * of this method.
     *
     * @param queryParameters
     *            the query parameters the target is resolved with, not
     *            {@code null}
     * @return the resolved title, or an empty {@link Optional} if the target
     *         declares no title and no default generator is available
     */
    public Optional<String> getPageTitle(QueryParameters queryParameters) {
        VaadinService service = VaadinService.getCurrent();
        Instantiator instantiator = service != null ? service.getInstantiator()
                : null;
        return RouteUtil.resolvePageTitle(instantiator, navigationTarget,
                routeParameters, queryParameters);
    }
}
