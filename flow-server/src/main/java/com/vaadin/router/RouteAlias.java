/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.router;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.ui.UI;

/**
 * Defines the route alias for components that function as navigation targets in
 * routing.
 * <p>
 * The route alias allows to declare several route paths in addition to the path
 * declared by the {@link @Route #Route} annotation. Component has to have at
 * least one {@link @Route #Route} annotation which is considered as a primary
 * route and its route path will be used if {@link Router#getUrl(Class)} is
 * called. Thus {@code @RouteAlias} route path is used only to resolve the
 * component during navigation.
 * <p>
 * This annotation is repeatable so you can use this annotation multiple times.
 *
 * @see Route
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@Repeatable(InternalContainerAnnotationForRoute.class)
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
     * component instance is reused.
     *
     * @return the layout component class used by the route target component.
     *         The default is the {@link UI} of the application.
     */
    Class<? extends RouterLayout> layout() default UI.class;

    /**
     * Have the rout chain break on defined class and not take into notice any
     * more parent layout route prefixes.
     *
     * @return route up to here should be absolute
     */
    boolean absolute() default false;
}
