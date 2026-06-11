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

/**
 * Resolves the logical parent of a route dynamically <em>without</em> requiring
 * an instance of either the route or its parent.
 * <p>
 * This is the dynamic counterpart of a static {@link RouteParent#value()}. The
 * logical parent it returns is used to build navigation hierarchies, such as
 * breadcrumb trails, where the whole chain of ancestor routes needs to be known
 * without instantiating any of them. It mirrors {@link PageTitleGenerator}: the
 * parent is resolved purely from the navigation target class and its
 * {@link RouteParameters}.
 * <p>
 * A resolver is referenced from a route through {@link RouteParent#resolver()}:
 *
 * <pre>
 * &#64;Route("orgs/:orgId/projects/:projectId")
 * &#64;RouteParent(resolver = OrgParentResolver.class)
 * public class ProjectView extends Div {
 *     // ...
 * }
 *
 * public class OrgParentResolver implements RouteParentResolver {
 *     &#64;Override
 *     public Optional&lt;RouteParentReference&gt; resolveParent(
 *             RouteParentContext context) {
 *         // carry over only the parameters the parent route needs
 *         RouteParameters parentParameters = new RouteParameters("orgId",
 *                 context.routeParameters().get("orgId").orElseThrow());
 *         return Optional.of(
 *                 new RouteParentReference(OrgView.class, parentParameters));
 *     }
 * }
 * </pre>
 *
 * Returning an empty {@link Optional} marks the top of the hierarchy.
 * <p>
 * Implementations must be stateless and are expected to be cheap to create:
 * they are instantiated through the application {@code Instantiator} (so
 * dependency injection is available) every time a parent is resolved, never the
 * route itself.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface RouteParentResolver extends Serializable {

    /**
     * Resolves the logical parent of the route described by the given context.
     * <p>
     * The returned reference carries both the parent navigation target and the
     * {@link RouteParameters} it should be resolved with. The resolver is
     * responsible for mapping the child route parameters to the subset the
     * parent route expects.
     *
     * @param context
     *            the context describing the route whose parent is resolved and
     *            its route parameters, not {@code null}
     * @return the logical parent reference, or an empty {@link Optional} if the
     *         route has no logical parent (it is a hierarchy root)
     */
    Optional<RouteParentReference> resolveParent(RouteParentContext context);
}
