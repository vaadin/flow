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
 * Defines menu information for a route for automatically populated menu.
 * <p>
 * {@link Menu} is used together with {@link Route} to include it automatically
 * in Hilla application's main menu, but only if server route is accessible and
 * {@code frontend/views/@layout.tsx} is used with {@code createMenuItems()}
 * function to build the menu.
 * </p>
 *
 * @see Route
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Menu {

    /**
     * Title to use in the menu. Falls back the page title if not defined.
     *
     * @return the title of the item in the menu. Empty String by default.
     */
    String title() default "";

    /**
     * Used to determine the order in the menu. Ties are resolved based on the
     * used title. Entries without explicitly defined ordering are put below
     * entries with an order. {@link Double#MIN_VALUE} is the default value and
     * considered as undefined.
     *
     * @return the order of the item in the menu. {@link Double#MIN_VALUE} by
     *         default.
     */
    double order() default Double.MIN_VALUE;

    /**
     * Icon to use in the menu. Value can go inside a {@code <vaadin-icon>}
     * element's {@code icon} attribute which accepts icon group and name like
     * 'vaadin:file'. Or it can go to a {@code <vaadin-icon>} element's
     * {@code src} attribute which takes path to the icon. E.g.
     * 'line-awesome/svg/lock-open-solid.svg'.
     *
     * @return A String for an icon. Empty String by default.
     */
    String icon() default "";
}
