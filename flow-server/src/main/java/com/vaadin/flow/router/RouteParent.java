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

import com.vaadin.flow.component.Component;

/**
 * Defines the <em>logical</em> parent of a navigation target.
 * <p>
 * This is a purely logical hierarchy used to build navigation aids such as
 * breadcrumb trails and hierarchical menus. It is independent of the layout
 * chain declared through {@link Route#layout()} and {@link RoutePrefix}: a
 * route may be rendered inside one layout while logically belonging under a
 * completely different route.
 * <p>
 * The parent can be defined either as a static {@link #value()} or, when it
 * needs to be computed, through a {@link #resolver()}. Both are resolved
 * without creating an instance of the route or its parent, which makes them
 * usable for routes that are not currently shown, such as the ancestors of a
 * breadcrumb trail.
 *
 * @author Vaadin Ltd
 * @since 25.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RouteParent {

    /**
     * Gets the static logical parent navigation target.
     * <p>
     * The parent inherits the {@link RouteParameters} of the annotated route,
     * narrowed to the names the parent's own route template declares (so a
     * parent with fewer or no parameters still resolves to a working link). Use
     * a {@link #resolver()} when the parent or its parameters need to be
     * computed differently.
     * <p>
     * Ignored when a {@link #resolver()} is set. Defaults to {@link Component}
     * itself, which marks "no static parent".
     *
     * @return the logical parent navigation target, or {@link Component} when
     *         no static parent is used
     */
    Class<? extends Component> value() default Component.class;

    /**
     * Gets the {@link RouteParentResolver} that resolves the logical parent
     * dynamically without requiring an instance of the route or its parent.
     * <p>
     * When set to a resolver other than the default {@link RouteParentResolver}
     * marker, the resolver is used to resolve the parent and {@link #value()}
     * is ignored. This allows the parent to be computed from the route
     * parameters even for routes that are not instantiated.
     *
     * @return the resolver type, or {@link RouteParentResolver} itself when no
     *         resolver is used
     */
    Class<? extends RouteParentResolver> resolver() default RouteParentResolver.class;
}
