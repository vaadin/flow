/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
