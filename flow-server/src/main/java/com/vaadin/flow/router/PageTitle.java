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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the page title for a navigation target.
 * <p>
 * This defines a static title. For a title that needs to be computed without
 * creating an instance of the navigation target (for example for the routes of
 * a breadcrumb trail or a menu), annotate the route with
 * {@link DynamicPageTitle} instead; the {@link #value()} declared here is then
 * available to the generator as a key. For a title that depends on the live
 * state of an already shown view, implement {@link HasDynamicTitle}.
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PageTitle {

    /**
     * Gets the title that should be used.
     * <p>
     * Empty string will clear any previous page title. In that case the browser
     * will decide what to show as the title, most likely the url.
     * <p>
     * When the route is also annotated with {@link DynamicPageTitle}, this
     * value is handed to the generator through {@link PageTitleContext#value()}
     * rather than used as the title directly, so a generic generator can use it
     * as a key (for example an i18n message key) to resolve the title.
     * <p>
     * You may dynamically update the title for an already shown view by
     * implementing the {@link HasDynamicTitle#getPageTitle()} method.
     *
     * @return a page title string
     */
    String value();
}
