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
package com.vaadin.flow.server.menu;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.Component;

/**
 * Menu entry for the main menu.
 * <p>
 * An entry may carry nested {@link #children() children} when obtained from
 * {@link MenuConfiguration#getMenuEntriesTree()}, forming a hierarchical menu.
 * The flat {@link MenuConfiguration#getMenuEntries()} returns entries with no
 * children.
 *
 * @param path
 *            the path to navigate to
 * @param title
 *            the title to display
 * @param order
 *            the order in the menu or null for default order
 * @param icon
 *            Icon to use in the menu or null for no icon. Value can go inside a
 *            {@code <vaadin-icon>} element's {@code icon} attribute which
 *            accepts icon group and name like 'vaadin:file'. Or it can go to a
 *            {@code <vaadin-icon>} element's {@code src} attribute which takes
 *            path to the icon. E.g. 'line-awesome/svg/lock-open-solid.svg'.
 * @param menuClass
 *            the source class with {@link com.vaadin.flow.router.Menu}
 *            annotation or null if not available. Always null for
 *            Hilla/TypeScript client views.
 * @param children
 *            the entries nested under this entry, never {@code null} but empty
 *            for a flat (non-hierarchical) entry
 */
public record MenuEntry(String path, String title, Double order, String icon,
        Class<? extends Component> menuClass,
        List<MenuEntry> children) implements Serializable {

    /**
     * Creates a flat menu entry with no children.
     *
     * @param path
     *            the path to navigate to
     * @param title
     *            the title to display
     * @param order
     *            the order in the menu or null for default order
     * @param icon
     *            the icon to use in the menu or null for no icon
     * @param menuClass
     *            the source {@code @Menu} class or null if not available
     */
    public MenuEntry(String path, String title, Double order, String icon,
            Class<? extends Component> menuClass) {
        this(path, title, order, icon, menuClass, List.of());
    }

    /**
     * Normalizes {@code children} to a non-null, unmodifiable list so that
     * {@link #children()} is never {@code null} (e.g. after deserialization of
     * an entry written before children existed).
     */
    public MenuEntry {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
