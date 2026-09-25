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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;

/**
 * Defines menu information for a route that should appear in an automatically
 * populated menu.
 * <p>
 * Use {@link Menu} together with {@link Route} so the route is included when
 * the application builds its main menu, for example with
 * {@link com.vaadin.flow.server.menu.MenuConfiguration#getMenuEntries()}. The
 * route is listed only when it is accessible.
 * <p>
 * If you also use Hilla, the same annotation is picked up for the Hilla main
 * menu when {@code frontend/views/@layout.tsx} calls {@code createMenuItems()}.
 *
 * @see Route
 * @since 24.4
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

    /**
     * The menu entry to nest this entry under in a hierarchical menu, such as
     * the one built from
     * {@link com.vaadin.flow.server.menu.MenuConfiguration#getMenuEntriesTree()}.
     * <p>
     * Use this when the menu hierarchy does not match the route hierarchy: by
     * default an entry is nested under the route it logically belongs to
     * (defined by {@link RouteParent}, or derived from the route URL), and
     * defining a parent here overrides that for the menu only. It has no effect
     * on navigation or on the flat
     * {@link com.vaadin.flow.server.menu.MenuConfiguration#getMenuEntries()}.
     * <p>
     * The parent does not have to be a direct route ancestor of the annotated
     * route. If the given parent is not itself part of the menu, the entry is
     * nested under the nearest ancestor of that parent which is, or becomes a
     * root entry when there is none.
     *
     * @return the menu parent navigation target, or {@link Component} itself,
     *         the default, when the route hierarchy should be used
     * @since 25.3
     */
    Class<? extends Component> parent() default Component.class;
}
