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

/**
 * Resolves a navigation target title dynamically <em>without</em> requiring an
 * instance of the navigation target.
 * <p>
 * This is the stateless counterpart of {@link HasDynamicTitle}. While
 * {@link HasDynamicTitle#getPageTitle()} is an instance method and therefore
 * only usable once the view has been created, a {@code PageTitleGenerator} is
 * resolved purely from the navigation target class and its
 * {@link RouteParameters}. This makes it suitable for use cases that need the
 * title of a route that is not (and should not be) instantiated, such as
 * breadcrumbs, menus or other navigation aids that render the titles of a whole
 * trail of routes.
 * <p>
 * A generator is referenced from a route through {@link DynamicPageTitle}:
 *
 * <pre>
 * &#64;Route("products/:productId")
 * &#64;DynamicPageTitle(ProductTitleGenerator.class)
 * public class ProductView extends Div {
 *     // ...
 * }
 *
 * public class ProductTitleGenerator implements PageTitleGenerator {
 *     &#64;Override
 *     public String generatePageTitle(PageTitleContext context) {
 *         String id = context.routeParameters().get("productId").orElse("");
 *         return "Product " + id;
 *     }
 * }
 * </pre>
 *
 * Implementations must be stateless and are expected to be cheap to create:
 * they are instantiated through the application {@code Instantiator} (so
 * dependency injection is available) every time a title is resolved, never the
 * navigation target itself.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface PageTitleGenerator extends Serializable {

    /**
     * Generates the title for the navigation target described by the given
     * context.
     *
     * @param context
     *            the context describing the navigation target and its route
     *            parameters, not {@code null}
     * @return the title to use for the navigation target, not {@code null}
     */
    String generatePageTitle(PageTitleContext context);
}
