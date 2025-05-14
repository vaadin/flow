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

package com.vaadin.flow.server.menu;

import java.io.Serializable;

import com.vaadin.flow.component.Component;

/**
 * Menu entry for the main menu.
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
 */
public record MenuEntry(String path, String title, Double order, String icon,
        Class<? extends Component> menuClass) implements Serializable {
}
