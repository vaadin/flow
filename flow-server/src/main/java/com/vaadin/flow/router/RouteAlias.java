/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.UI;

/**
 * Defines the route alias for components that function as navigation targets in
 * routing.
 * <p>
 * The route alias allows declaring several route paths in addition to the path
 * declared by the {@link Route} annotation. The component has to have at least
 * one {@literal @Route} annotation which is considered as a primary route and
 * its route path will be used if {@link Router#getUrl(Class)} is called. Thus
 * {@code @RouteAlias} route path is used only to resolve the component during
 * navigation.
 * <p>
 * This annotation can be used multiple times on the same class.
 *
 * @see Route
 * @see RoutePrefix
 * @see RouterLayout
 * @see UI
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@Repeatable(RouteAlias.Container.class)
public @interface RouteAlias {

    /**
     * Gets the route alias path value of the annotated class.
     *
     * @return the path value of this route
     */
    String value();

    /**
     * Sets the parent component for the route target component.
     * <p>
     * When navigating between components that use the same layout, the same
     * component instance is reused. Default layout target is the {@link UI},
     * but the layout should not be a custom {@code UI} as {@code UI} is a special
     * class used to know where the route stack ends and no parent layouts should
     * be involved.
     *
     * <p>
     * All layout stacks will be appended to the {@code UI} as it represents
     * the Body element.
     *
     * @return the layout component class used by the route target component.
     * @see RouterLayout
     */
    Class<? extends RouterLayout> layout() default UI.class;

    /**
     * Have the route chain break on defined class and not take into notice any
     * more parent layout route prefixes.
     *
     * @return route up to here should be absolute
     */
    boolean absolute() default false;

    /**
     * Internal annotation to enable use of multiple {@link RouteAlias}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @Inherited
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link RouteAlias}
         * annotations.
         *
         * @return an array of the RouteAlias annotations
         */
        RouteAlias[] value();
    }

}
