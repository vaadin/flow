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
 * Allows to resolve navigation target title dynamically at runtime, based on
 * the live state of the navigation target instance.
 * <p>
 * Because {@link #getPageTitle()} is an instance method, it is resolved from
 * the view that is actually shown and can therefore reflect state that only
 * exists once the view has been created and navigated to. Use this interface
 * when the title depends on such live state, for example:
 * <ul>
 * <li>a value looked up while building the view and cached in an instance
 * field, so the lookup is not repeated only to compute the title;</li>
 * <li>a title that changes after navigation while the view is shown;</li>
 * <li>a title that depends on the view's own access control or navigation
 * awareness.</li>
 * </ul>
 * Since it needs an instance, it can only produce a title for a route that is
 * actually shown. To resolve a title <em>without</em> an instance &mdash; for
 * example for routes that are not shown, such as the entries of a breadcrumb
 * trail or a menu &mdash; use the instance-free counterpart
 * {@link DynamicPageTitle} together with a {@link PageTitleGenerator} instead.
 * <p>
 * NOTE: It is not legal for a class to both implement {@link HasDynamicTitle}
 * and have a {@link PageTitle} annotation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @see DynamicPageTitle
 * @see PageTitleGenerator
 */
@FunctionalInterface
public interface HasDynamicTitle extends Serializable {

    /**
     * Gets the title of this navigation target.
     *
     * @return the title of this navigation target
     */
    String getPageTitle();
}
