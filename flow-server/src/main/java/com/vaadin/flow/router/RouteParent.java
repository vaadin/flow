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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;

/**
 * Declares the conceptual parent route of a {@link Route @Route}-annotated
 * navigation target.
 * <p>
 * This annotation expresses a route-hierarchy relationship: "the conceptual
 * parent of this route is that route". It is consumed by navigation components
 * (e.g. breadcrumbs, back-navigation helpers, sitemap renderers) that need to
 * walk ancestor routes for a given view. It does not affect how the view is
 * rendered &mdash; visual nesting is configured via {@link Route#layout()} and
 * {@link ParentLayout}.
 * <p>
 * When this annotation is present on a route class, hierarchy-walking consumers
 * use {@link #value()} as the parent. When it is absent, those consumers
 * typically fall back to URL-prefix walking. The parent class should itself be
 * annotated with {@link Route @Route}; if it is not, consumers fall back to
 * URL-prefix walking for that step.
 *
 * @see Route
 * @see ParentLayout
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RouteParent {

    /**
     * The view class that is the conceptual parent of the annotated
     * {@link Route @Route} class.
     *
     * @return the conceptual parent route class
     */
    Class<? extends Component> value();
}
