/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link RouterLayout} as eligible for use as automatic layout for
 * Flow and Hilla views.
 * <p>
 * Eligibility is checked by the value as path match.
 * <p>
 * Opting out from automatic layouting for a {@link Route} or {@link RouteAlias}
 * can be done with the {@link Route#autoLayout()} or
 * {@link RouteAlias#autoLayout()} method.
 *
 * @see Route
 * @see RouteAlias
 * @see RouterLayout
 * @since 24.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Layout {

    /**
     * Sets the layout identifier value to link client view to server layout
     * component. Default is "/".
     * <p>
     * Layout is linked by path so that anything matching the whole start match
     * the rest. so "" or "/" matches all paths, but "/view" matches all paths
     * starting with view path part so "/view/**".
     * <p>
     * Note! context path is not taken into account.
     *
     *
     * @return the set layout identifier value
     */
    String value() default "/";
}
