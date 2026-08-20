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
 * NOTE: It is not legal for a class to both implement {@link HasDynamicTitle}
 * and have a {@link PageTitle} annotation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @deprecated Prefer {@link DynamicPageTitle} together with a
 *             {@link PageTitleGenerator} instead. Because
 *             {@link #getPageTitle()} is an instance method, it can only
 *             produce a title for a route that is actually shown, while a
 *             {@link PageTitleGenerator} resolves the title from the navigation
 *             target class and its {@link RouteParameters} without an instance.
 *             That makes it usable for routes that are not (and should not be)
 *             instantiated, such as the entries of a breadcrumb trail or a
 *             menu, so the same title logic also applies to those navigation
 *             aids. Use this interface only when the title genuinely needs to
 *             reflect the live state of an already shown view.
 */
@Deprecated(since = "25.3")
@FunctionalInterface
public interface HasDynamicTitle extends Serializable {

    /**
     * Gets the title of this navigation target.
     *
     * @return the title of this navigation target
     */
    String getPageTitle();
}
