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
 * The title can be defined either as a static {@link #value()} or, when it
 * needs to be computed, through a {@link #generator()}. A generator resolves
 * the title without creating an instance of the navigation target, which makes
 * it usable for routes that are not currently shown (for example the routes of
 * a breadcrumb trail or a menu). For a title that depends on the live state of
 * an already shown view, implement {@link HasDynamicTitle} instead.
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PageTitle {

    /**
     * Gets the static title that should be used.
     * <p>
     * Empty string will clear any previous page title. In that case the browser
     * will decide what to show as the title, most likely the url.
     * <p>
     * Ignored when a {@link #generator()} is set.
     * <p>
     * You may dynamically update the title for an already shown view by
     * implementing the {@link HasDynamicTitle#getPageTitle()} method.
     *
     * @return a page title string
     */
    String value() default "";

    /**
     * Gets the {@link PageTitleGenerator} that resolves the title dynamically
     * without requiring an instance of the navigation target.
     * <p>
     * When set to a generator other than the default {@link PageTitleGenerator}
     * marker, the generator is used to resolve the title and {@link #value()}
     * is ignored. This allows the title to be computed from the route
     * parameters even for routes that are not instantiated, such as the entries
     * of a breadcrumb trail.
     *
     * @return the generator type, or {@link PageTitleGenerator} itself when no
     *         generator is used
     */
    Class<? extends PageTitleGenerator> generator() default PageTitleGenerator.class;
}
